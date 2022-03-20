package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.LogUtil
import java.util.ArrayDeque
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface StageName
enum class DefaultName : StageName { BEFORE, HTTP, ERROR, AFTER }

data class Stage(
    val name: StageName,
    val ignoresExceptions: Boolean = false, // tasks in this scope should be executed even if some previous stage ended up with exception
    val stageInitializer: StageInitializer // DLS method to add task to the stage's queue
)

data class Task(val stage: Stage, val handler: TaskHandler)
typealias TaskHandler = (JavalinServletHandler) -> Unit

typealias SubmitTask = (TaskHandler) -> Unit
typealias StageInitializer = JavalinServletHandler.(submitTask: SubmitTask) -> Unit

class JavalinServletHandler(
    private val stages: Iterator<Stage>,
    private val config: JavalinConfig,
    private val errorMapper: ErrorMapper,
    private val exceptionMapper: ExceptionMapper,
    val ctx: Context,
    val type: HandlerType,
    val requestUri: String,
    val responseWrapperContext: ResponseWrapperContext,
    val request: HttpServletRequest,
    var response: HttpServletResponse
) {

    private val tasks = ArrayDeque<Task>(8)
    private var currentStage: CompletableFuture<*> = emptySyncStage()
    private var asyncContext: AsyncContext? = null
    private var latestResultFuture: CompletableFuture<*>? = null // future defined by user in Context, we have to keep this only for timeout listener
    private var errored = false
    private val finished = AtomicBoolean(false) // requires support for atomic switch

    internal fun queueNextTask() {
        if (refillTasks()) { // finish response, if there is no more tasks
            return finishResponse()
        }

        this.currentStage = tasks.poll()
            .let { task -> currentStage.thenCompose { executeTask(task) } }
            .exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // default catch-all for whole scope, might occur when e.g. finishResponse() will fail
    }

    private fun executeTask(task: Task): CompletableFuture<Void?> {
        if (errored && !task.stage.ignoresExceptions) { // skip handlers that don't support errored pipeline
            return emptySyncStage().thenAccept { queueNextTask() }
        }

        try {
            task.handler(this)
        } catch (exception: Exception) {
            this.errored = true
            exceptionMapper.handle(exception, ctx) // still can throw errors that occurred in catch body
        }

        return handleAsync().thenAccept { queueNextTask() } // move to next available handler in the pipeline
    }

    private fun refillTasks(): Boolean {
        while (tasks.isEmpty() && stages.hasNext()) { // refill tasks from a next stage only if the current pool is empty
            stages.next().also { stage ->
                stage.stageInitializer(this) { tasks.offer(Task(stage, it)) } // add tasks from stage to handler's tasks queue
            }
        }
        return tasks.isEmpty()
    }

    private fun handleAsync(): CompletableFuture<Void?> =
        ctx.asyncTaskReference.getAndSet(null) // consume future value
            ?.also { latestResultFuture = it } // cache the latest future to provide a possibility to cancel this in timeout listener
            ?.also { if (asyncContext == null) this.asyncContext = startAsync() } // enable async context
            ?.thenAccept { result -> ctx.futureConsumer?.accept(result) } // future post-processing, this consumer can set result, status, etc
            ?.exceptionally { exceptionMapper.handleFutureException(ctx, it) } // standard exception handler
            ?: emptySyncStage() // sync stub

    private fun startAsync(): AsyncContext = request.startAsync().also {
        this.response = it.response as HttpServletResponse
        it.timeout = config.asyncRequestTimeout
        it.addTimeoutListener { // the timeout kinda escapes the pipeline, so we need to shut down it manually with: cancel -> error message -> error handling -> finishing response
            currentStage.cancel(true) // cancel current flow
            latestResultFuture?.cancel(true) // cancel latest user future (futures does not propagate cancel request to their dependencies)
            ctx.status(500).result("Request timed out")
            errorMapper.handle(ctx.status(), ctx)
            finishResponse()
        }
    }

    private fun finishResponse() {
        if (finished.getAndSet(true)) return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        try {
            JavalinResponseWrapper(response, responseWrapperContext).write(ctx.resultStream())
            config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable) // handle any unexpected error, e.g. write failure
        } finally {
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

private fun emptySyncStage(): CompletableFuture<Void?> = CompletableFuture.completedFuture(null) // creates completed stage that behaves like sync request until it's replaced by future result
