package io.javalin.http

import io.javalin.core.util.LogUtil
import java.util.ArrayDeque
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal typealias LayerHandler = (JavalinServletFlow) -> Unit

internal data class Scope(
    val name: String,
    val allowsErrors: Boolean = false,
    private val initialize: JavalinFlowContext.(submitTask: (LayerHandler) -> Unit) -> Unit
) {

    fun initialize(queue: Queue<Pair<Scope, LayerHandler>>, context: JavalinFlowContext) =
        initialize(context) { task -> queue.offer(Pair(this, task)) }

}

internal data class JavalinFlowContext(
    val ctx: Context,
    val type: HandlerType,
    val requestUri: String,
    val responseWrapperContext: ResponseWrapperContext
)

internal class JavalinServletFlow(
    private val scopes: List<Scope>,
    private val servlet: JavalinServlet,
    private val context: JavalinFlowContext,
    val request: HttpServletRequest,
    var response: HttpServletResponse
) {

    // Flow state
    private var currentStage: CompletableFuture<*> = syncStage() // main stage used to pipeline handlers as a chain
    private var currentScope = 0
    private val queuedHandlers = ArrayDeque<Pair<Scope, LayerHandler>>(scopes.size * 2) // as long as timeout listener does not touch this pipeline there is no need to use thread-safe structure
    // Request state
    private var asyncContext: AsyncContext? = null
    private var errored = false
    private val finished = AtomicBoolean(false) // requires support for atomic switch
    private var latestFuture: CompletableFuture<*>? = null
    // Utility values
    private val ctx: Context = context.ctx
    private val exceptionMapper = servlet.exceptionMapper

    fun start() =
        continueFlow() // Start request lifecycle

    private fun continueFlow() {
        if (queuedHandlers.isEmpty()) {
            while (currentScope < scopes.size && queuedHandlers.isEmpty()) {
                val scope = scopes[currentScope++]
                scope.initialize(queuedHandlers, context)
            }
            if (currentScope == scopes.size && queuedHandlers.isEmpty()) { // if scopes & pipelines are empty, it means that response has been finished
                finishResponse()
                return
            }
        }

        val (scope, handler) = queuedHandlers.poll()

        this.currentStage = currentStage.thenCompose {
            if (errored && scope.allowsErrors.not()) { // skip handlers that don't support errored pipeline
                return@thenCompose syncStage().thenAccept { continueFlow() }
            }

            try {
                handler(this)
            } catch (exception: Exception) {
                this.errored = true
                exceptionMapper.handle(exception, ctx) // still can throw errors that occurred in catch body
            }

            val resultFuture = ctx.async.getAndSet(null) // consume future value
                ?.also { latestFuture = it } // cache the latest future to provide a possibility to cancel this in timeout listener
                ?.also { if (asyncContext == null) startAsync() } // enable async context
                ?.exceptionally { exceptionMapper.handleFutureException(ctx, it) } // handle standard exceptions
                ?.thenAccept { result -> ctx.futureConsumer?.accept(result) } // future post-processing, this consumer can set result, status, etc
                ?.exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // exception might occur when writing response/in future handler
                ?: syncStage() // stub future for sync & completed async requests

            return@thenCompose resultFuture.thenAccept { continueFlow() } // move to next available handler in the pipeline
        }
        .exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // default catch-all for whole scope
    }

    private fun startAsync() {
        this.asyncContext = request.startAsync().also {
            this.response = it.response as HttpServletResponse
            it.timeout = servlet.config.asyncRequestTimeout
            it.addTimeoutListener { // the timeout kinda escapes the pipeline, so we need to shut down it manually with: cancel -> error message -> error handling -> finishing response
                currentStage.cancel(true) // cancel current flow
                latestFuture?.cancel(true) // cancel latest user future (futures does not propagate cancel request to their dependencies)
                with(ctx) {
                    status(500).result("Request timed out")
                    servlet.handleError(this)
                }
                finishResponse()
            }
        }
    }

    private fun finishResponse() {
        if (finished.getAndSet(true)) {
            return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        }
        try {
            JavalinResponseWrapper(response, context.responseWrapperContext).write(ctx.resultStream())
            servlet.config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable) // handle any unexpected error, e.g. write failure
        }
        finally {
            asyncContext?.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

    private fun syncStage(): CompletableFuture<Void?> =
        CompletableFuture.completedFuture(null) // creates completed stage that behaves like sync request until it's not replaced by future result

}

private fun AsyncContext.addTimeoutListener(callback: () -> Unit) = this.addListener(object : AsyncListener {
    override fun onComplete(event: AsyncEvent) {}
    override fun onError(event: AsyncEvent) {}
    override fun onStartAsync(event: AsyncEvent) {}
    override fun onTimeout(event: AsyncEvent) = callback() // this is all we care about
})
