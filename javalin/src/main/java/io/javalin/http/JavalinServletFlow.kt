package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.LogUtil
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal typealias LayerHandler = () -> Unit

internal data class Scope(
    val name: String,
    val allowsErrors: Boolean = false,
    private val initialize: Scope.(Queue<Pair<Scope, LayerHandler>>) -> Unit
) {

    fun initialize(queue: Queue<Pair<Scope, LayerHandler>>) =
        initialize(this, queue)

    fun submit(queue: Queue<Pair<Scope, LayerHandler>>, task: LayerHandler) =
        queue.offer(Pair(this, task))

}

internal data class JavalinFlowContext(
    val type: HandlerType,
    val responseWrapperContext: ResponseWrapperContext,
    val requestUri: String,
    val ctx: Context
)

internal class JavalinServletFlow(
    private val servlet: JavalinServlet,
    private val context: JavalinFlowContext,
    private val request: HttpServletRequest,
    private var response: HttpServletResponse,
    rawScopes: List<Scope>
) {

    // Utilities pulled out of dependencies
    private val config: JavalinConfig = servlet.config
    private val exceptionMapper: ExceptionMapper = servlet.exceptionMapper
    private val ctx = context.ctx

    private val scopes = ConcurrentLinkedQueue(rawScopes) // Scopes with async request handling support
    private var flow: CompletableFuture<*> = CompletableFuture.completedFuture(Unit).exceptionally { servlet.exceptionMapper.handle(it, ctx) } // Main stage used to pipeline handlers as a chain
    private val handlerPipeline = ConcurrentLinkedQueue<Pair<Scope, LayerHandler>>()
    private val finished = AtomicBoolean(false) // requires support for atomic switch
    private var asyncContext: AsyncContext? = null
    private var latestFuture: CompletableFuture<*>? = null
    private var errored = false

    fun start() =
        continueFlow()

    private fun syncStage(): CompletionStage<Void?> =
        CompletableFuture.completedFuture(null) // creates completed stage that behaves like sync request until it's not replaced by future result

    private fun continueFlow() {
        if (handlerPipeline.isEmpty()) {
            while (scopes.isNotEmpty() && handlerPipeline.isEmpty()) {
                val currentScope = scopes.poll()
                currentScope.initialize(handlerPipeline)
            }
            if (scopes.isEmpty() && handlerPipeline.isEmpty()) { // if scopes & pipelines are empty, it means that response has been finished
                finishResponse()
                return
            }
        }

        val (scope, handler) = handlerPipeline.poll()

        this.flow = flow.thenCompose {
            if (errored && scope.allowsErrors.not()) { // skip handlers that don't support errored pipeline
                return@thenCompose syncStage().thenAccept { continueFlow() }
            }

            try {
                handler()
            } catch (exception: Exception) {
                this.errored = true
                exceptionMapper.handle(exception, ctx) // still can throw errors that occurred in catch body
            }

            val resultFuture = ctx.async.getAndSet(null)
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
            it.timeout = config.asyncRequestTimeout
            it.addTimeoutListener { // the timeout kinda escapes the pipeline, so we need to shut down it manually with: cancel -> error message -> error handling -> finishing response
                flow.cancel(true) // cancel current flow
                latestFuture?.cancel(true) // cancel latest user future (futures does not propagate cancel request to their dependencies)
                ctx.status(500).result("Request timed out")
                servlet.handleError(ctx)
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
            config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            servlet.exceptionMapper.handleUnexpectedThrowable(response, throwable) // handle any unexpected error, e.g. write failure
        }
        finally {
            asyncContext?.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}

private fun AsyncContext.addTimeoutListener(callback: () -> Unit) = this.addListener(object : AsyncListener {
    override fun onComplete(event: AsyncEvent) {}
    override fun onError(event: AsyncEvent) {}
    override fun onStartAsync(event: AsyncEvent) {}
    override fun onTimeout(event: AsyncEvent) = callback() // this is all we care about
})
