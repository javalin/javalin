package io.javalin.http

import io.javalin.core.util.LogUtil
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal fun interface Task {
    fun execute(context: JavalinServletHandler)
}

internal data class Cycle(
    val name: String,
    val ignoresExceptions: Boolean = false, // tasks in this scope should be executed even if some previous cycle ended up with exception
    val tasksInitialization: JavalinServletHandler.(submitTask: (Task) -> Unit) -> Unit // DLS method to add task to the cycle's queue
)

internal class JavalinServletHandler(
    lifecycle: List<Cycle>,
    private val servlet: JavalinServlet,
    private val exceptionMapper: ExceptionMapper = servlet.exceptionMapper,
    val ctx: Context,
    val type: HandlerType,
    val requestUri: String,
    val responseWrapperContext: ResponseWrapperContext,
    val request: HttpServletRequest,
    var response: HttpServletResponse
) {

    private val cycles = lifecycle.iterator()
    private val tasks = ArrayDeque<Pair<Cycle, Task>>(lifecycle.size * 2)
    private var currentTask: CompletableFuture<*> = emptyStage()
    private var asyncContext: AsyncContext? = null
    private var errored = false
    private val finished = AtomicBoolean(false) // requires support for atomic switch
    private var latestFuture: CompletableFuture<*>? = null

    fun execute() =
        executeNextTask()

    private fun executeNextTask() {
        while (tasks.isEmpty() && cycles.hasNext()) {
            cycles.next().also {
                it.tasksInitialization(this) { task -> tasks.offer(Pair(it, task)) } // add tasks from cycle to handler's tasks queue
            }
        }
        if (tasks.isEmpty()) {
            return finishResponse()
        }

        val (cycle, task) = tasks.poll()

        this.currentTask = currentTask.thenCompose {
            if (errored && !cycle.ignoresExceptions) { // skip handlers that don't support errored pipeline
                return@thenCompose emptyStage().thenAccept { executeNextTask() }
            }

            try {
                task.execute(this)
            } catch (exception: Exception) {
                this.errored = true
                exceptionMapper.handle(exception, ctx) // still can throw errors that occurred in catch body
            }

            val resultFuture = ctx.async.getAndSet(null) // consume future value
                ?.also { latestFuture = it } // cache the latest future to provide a possibility to cancel this in timeout listener
                ?.also { if (asyncContext == null) this.asyncContext = startAsync() } // enable async context
                ?.exceptionally { exceptionMapper.handleFutureException(ctx, it) } // handle standard exceptions
                ?.thenAccept { result -> ctx.futureConsumer?.accept(result) } // future post-processing, this consumer can set result, status, etc
                ?.exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // exception might occur when writing response/in future handler
                ?: emptyStage() // stub future for sync & completed async requests

            return@thenCompose resultFuture.thenAccept { executeNextTask() } // move to next available handler in the pipeline
        }
        .exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // default catch-all for whole scope
    }

    private fun startAsync(): AsyncContext = request.startAsync().also {
        this.response = it.response as HttpServletResponse
        it.timeout = servlet.config.asyncRequestTimeout
        it.addTimeoutListener { // the timeout kinda escapes the pipeline, so we need to shut down it manually with: cancel -> error message -> error handling -> finishing response
            currentTask.cancel(true) // cancel current flow
            latestFuture?.cancel(true) // cancel latest user future (futures does not propagate cancel request to their dependencies)
            with(ctx) {
                status(500).result("Request timed out")
                servlet.handleError(this)
            }
            finishResponse()
        }
    }

    private fun finishResponse() {
        if (finished.getAndSet(true)) return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        try {
            JavalinResponseWrapper(response, responseWrapperContext).write(ctx.resultStream())
            servlet.config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable) // handle any unexpected error, e.g. write failure
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

private fun emptyStage(): CompletableFuture<Void?> = CompletableFuture.completedFuture(null) // creates completed stage that behaves like sync request until it's not replaced by future result
