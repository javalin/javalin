package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.LogUtil
import java.util.*
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

/**
 * Executes request lifecycle.
 * The lifecycle consists of multiple [stages] (before/http/etc), each of which
 * can have one or more [tasks]. The default lifecycle is define in [JavalinServlet].
 * [JavalinServletHandler] is called only once per request, and has a mutable state.
 */
class JavalinServletHandler(
    private val stages: ArrayDeque<Stage>,
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

    /** Queue of tasks to execute within the current [Stage] */
    private val tasks = ArrayDeque<Task>(4)

    /** Future representing currently queued task */
    private var currentTaskFuture: CompletableFuture<*> = emptySyncStage()

    /** Async context, if it's null request is handled using standard sync stages (default behaviour) */
    private var asyncContext: AsyncContext? = null

    /** Caches future defined by user in Context, we have to keep this only for timeout listener */
    private var latestResultFuture: CompletableFuture<*>? = null //

    /** Indicates if exception occurred during execution of a tasks chain */
    private var errored = false

    /** Indicates if [JavalinServletHandler] already wrote response to client, requires support for atomic switch */
    private val finished = AtomicBoolean(false)

    /**
     * This method starts execution process of all stages in a given lifecycle.
     * Execution is based on recursive calls of this method,
     * because we need a lazy evaluation of next tasks in a chain to support multiple concurrent stages.
     */
    internal fun queueNextTask() {
        if (tasks.isEmpty()) {
            refillTasks()
            if (tasks.isEmpty()) return finishResponse() // we didn't find any more tasks, time to write the response
        }
        this.currentTaskFuture = this.currentTaskFuture
            .thenCompose { executeTask(tasks.poll()) } // wrap current task in future and chain into current future
            .exceptionally { exceptionMapper.handleUnexpectedThrowable(response, it) } // default catch-all for whole scope, might occur when e.g. finishResponse() will fail
    }

    /** Executes the given task with proper error handling and returns next task to execute as future */
    private fun executeTask(task: Task): CompletableFuture<Void> {
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

    /** Refill [tasks] if [tasks] is empty (and there are more [stages] with tasks) */
    private fun refillTasks() {
        while (tasks.isEmpty() && stages.isNotEmpty()) { // refill tasks from a next stage only if the current pool is empty
            stages.poll().also { stage ->
                stage.stageInitializer(this) { tasks.offer(Task(stage, it)) } // add tasks from stage to handler's tasks queue
            }
        }
    }

    /** Fetches result future defined by user in [Context] and wraps it as a next task to execute in chain. */
    private fun handleAsync(): CompletableFuture<Void> =
        ctx.asyncTaskReference.getAndSet(null) // consume future value
            ?.also { latestResultFuture = it } // cache the latest future to provide a possibility to cancel this in timeout listener
            ?.also { if (asyncContext == null) this.asyncContext = startAsync() } // enable async context
            ?.thenAccept { result -> ctx.futureConsumer?.accept(result) } // future post-processing, this consumer can set result, status, etc
            ?.exceptionally { exceptionMapper.handleFutureException(ctx, it) } // standard exception handler
            ?: emptySyncStage() // sync stub

    /** Initializes async context for current request. */
    private fun startAsync(): AsyncContext = request.startAsync().also {
        this.response = it.response as HttpServletResponse
        it.timeout = config.asyncRequestTimeout
        it.addTimeoutListener { // the timeout kinda escapes the pipeline, so we need to shut down it manually with: cancel -> error message -> error handling -> finishing response
            currentTaskFuture.cancel(true) // cancel current flow
            latestResultFuture?.cancel(true) // cancel latest user future (futures does not propagate cancel request to their dependencies)
            ctx.status(500).result("Request timed out")
            errorMapper.handle(ctx.status(), ctx)
            finishResponse()
        }
    }

    /** Writes response to the client and frees resources */
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

/** Creates completed stage that behaves like sync request until it's replaced by future result */
private fun emptySyncStage(): CompletableFuture<Void> =
    CompletableFuture.completedFuture(null)
