package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.JavalinLogger
import io.javalin.core.util.LogUtil
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener

interface StageName
enum class DefaultName : StageName { BEFORE, HTTP, ERROR, AFTER }

data class Stage(
    val name: StageName,
    val ignoresExceptions: Boolean = false, // tasks in this scope should be executed even if some previous stage ended up with exception
    val initializer: StageInitializer // DLS method to add task to the stage's queue
)

internal data class Result(
    val previous: InputStream? = null,
    val future: CompletableFuture<Any?> = emptySyncStage(),
    val callback: Consumer<Any?>? = null,
)

data class Task(val stage: Stage, val handler: TaskHandler)
typealias TaskHandler = (JavalinServletHandler) -> Unit

typealias SubmitTask = (TaskHandler) -> Unit
typealias StageInitializer = JavalinServletHandler.(submitTask: SubmitTask) -> Unit

/**
 * Executes request lifecycle.
 * The lifecycle consists of multiple [stages] (before/http/etc), each of which
 * can have one or more [tasks]. The default lifecycle is defined in [JavalinServlet].
 * [JavalinServletHandler] is called only once per request, and has a mutable state.
 */
class JavalinServletHandler(
    private val stages: ArrayDeque<Stage>,
    private val config: JavalinConfig,
    private val errorMapper: ErrorMapper,
    private val exceptionMapper: ExceptionMapper,
    val ctx: Context,
    val type: HandlerType = HandlerType.fromServletRequest(ctx.req),
    val requestUri: String = ctx.req.requestURI.removePrefix(ctx.req.contextPath),
) {

    /** Queue of tasks to execute within the current [Stage] */
    private val tasks = ArrayDeque<Task>(4)

    /** Future representing currently queued task */
    private var currentTaskFuture: CompletableFuture<InputStream?> = emptySyncStage(null)

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
        if (tasks.isEmpty()) refillTasks()
        if (tasks.isEmpty()) return finishResponse() // we didn't find any more tasks, time to write the response

        currentTaskFuture = currentTaskFuture
            .thenCompose { executeTask(it, tasks.poll()) } // wrap current task in future and chain into current future
            .exceptionally { exceptionMapper.handleUnexpectedThrowable(ctx.res, it) } // default catch-all for whole scope, might occur when e.g. finishResponse() will fail
    }

    /** Executes the given task with proper error handling and returns next task to execute as future */
    private fun executeTask(previousResult: InputStream?, task: Task): CompletableFuture<InputStream?> {
        if (errored && !task.stage.ignoresExceptions) { // skip handlers that don't support errored pipeline
            return queueNextTask().run { emptySyncStage(previousResult) }
        }

        try {
            task.handler(this)
        } catch (exception: Exception) {
            errored = true
            exceptionMapper.handle(exception, ctx) // still can throw errors that occurred in catch body
        }

        return executeUserFuture(previousResult) // move to the next available handler in the pipeline
    }

    /** Fetches result future defined by user in [Context] or returns an empty stage */
    private fun executeUserFuture(previousResult: InputStream?): CompletableFuture<InputStream?> =
        ctx.resultReference.getAndSet(Result(previousResult))
            .also { result -> if (!ctx.isAsync() && !result.future.isDone) startAsyncAndAddDefaultTimeoutListeners() } // start async context only if the future is not already completed
            .also { result -> if (ctx.isAsync()) ctx.req.asyncContext.addTimeoutListener {
                JavalinLogger.info("╭∩╮(Ο_Ο)╭∩╮ ASYNC: Other timeout listener")
                result.future.cancel(true)
            } }
            .let { result ->
                result.future
                    .thenApply { (result.callback ?: defaultFutureCallback()).accept(it) } // user callback for when future resolves, this consumer can set result, status, etc
                    .thenApply { ctx.resultStream() ?: previousResult }
                    .exceptionally { throwable -> exceptionMapper.handleFutureException(ctx, throwable) } // standard exception handler
                    .thenApply { it.also { queueNextTask() } } // we have to attach the "also" to the input stream to avoid mapping a void
            }

    private fun defaultFutureCallback(): Consumer<Any?> = Consumer { result ->
        when (result) {
            is Unit -> {}
            is InputStream -> ctx.result(result)
            is String -> ctx.result(result)
            is Any -> ctx.json(result)
        }
    }

    private fun startAsyncAndAddDefaultTimeoutListeners() = ctx.req.startAsync()
        .addTimeoutListener { // a timeout avoids the pipeline - we need to handle it manually
            JavalinLogger.info("╭∩╮(Ο_Ο)╭∩╮ ASYNC: First timeout listener")
            currentTaskFuture.cancel(true) // cancel current task
            ctx.status(500).result("Request timed out") // default error handling
            errorMapper.handle(ctx.status(), ctx) // user defined error handling
            finishResponse() // write response
        }.also { it.timeout = config.asyncRequestTimeout }


    /** Writes response to the client and frees resources */
    private fun finishResponse() {
        if (finished.getAndSet(true)) return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        try {
            JavalinResponseWrapper(ctx, config).write(ctx.resultStream())
            config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(ctx.res, throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.isAsync()) ctx.req.asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

    /** Refill [tasks] if [tasks] is empty (and there are more [stages] with tasks) */
    private fun refillTasks() {
        while (tasks.isEmpty() && stages.isNotEmpty()) { // refill tasks from a next stage only if the current pool is empty
            val stage = stages.poll()
            stage.initializer.invoke(this) { tasks.offer(Task(stage, it)) } // add tasks from stage to handler's tasks queue
        }
    }

}

private fun AsyncContext.addTimeoutListener(callback: () -> Unit) = this.apply {
    addListener(object : AsyncListener {
        override fun onComplete(event: AsyncEvent) {}
        override fun onError(event: AsyncEvent) {}
        override fun onStartAsync(event: AsyncEvent) {}
        override fun onTimeout(event: AsyncEvent) = callback() // this is all we care about
    })
}

/** Checks if request is executed asynchronously */
private fun Context.isAsync(): Boolean = req.isAsyncStarted

/** Creates completed stage that behaves like sync request until it's replaced by future result */
fun <T : Any?> emptySyncStage(result: T? = null): CompletableFuture<T> = CompletableFuture.completedFuture(result)
