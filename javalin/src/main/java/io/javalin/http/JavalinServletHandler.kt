package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.LogUtil
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener

interface StageName
enum class DefaultName : StageName { BEFORE, HTTP, ERROR, AFTER }

data class Stage(
    val name: StageName,
    val haltsOnError: Boolean = true, // tasks in this scope should be executed even if some previous stage ended up with exception
    val initializer: StageInitializer // DSL method to add task to the stage's queue
)

internal data class Result(
    val previous: InputStream? = null,
    val future: CompletableFuture<*> = completedFuture(null),
    val callback: Consumer<Any?>? = null,
)

internal data class Task(
    val stage: Stage,
    val handler: TaskHandler
)

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
    val requestType: HandlerType = HandlerType.fromServletRequest(ctx.req),
    val requestUri: String = ctx.req.requestURI.removePrefix(ctx.req.contextPath),
) {

    /** Queue of tasks to execute within the current [Stage] */
    private val tasks = ArrayDeque<Task>(4)

    /** Future representing currently queued task */
    private var currentTaskFuture: CompletableFuture<InputStream?> = completedFuture(null)

    /** InputStream representing previous result */
    private var previousResult: InputStream? = null

    /** Indicates if exception occurred during execution of a tasks chain */
    private var errored = false

    /** Indicates if [JavalinServletHandler] already wrote response to client, requires support for atomic switch */
    private val finished = AtomicBoolean(false)

    /**
     * This method starts execution process of all stages in a given lifecycle.
     * Execution is based on recursive calls of this method,
     * because we need a lazy evaluation of next tasks in a chain to support multiple concurrent stages.
     */
    internal fun queueNextTaskOrFinish() {
        while (tasks.isEmpty() && stages.isNotEmpty()) { // refill tasks from next stage, if the current queue is empty
            val stage = stages.poll()
            stage.initializer.invoke(this) { taskHandler -> tasks.offer(Task(stage, taskHandler)) } // add tasks from stage to task queue
        }
        if (tasks.isEmpty())
            finishResponse() // we looked but didn't find any more tasks, time to write the response
        else
            currentTaskFuture = currentTaskFuture
                .thenAccept { inputStream -> previousResult = inputStream }
                .thenCompose { executeNextTask() } // chain next task into current future
                .exceptionally { throwable -> exceptionMapper.handleUnexpectedThrowable(ctx.res, throwable) } // default catch-all for whole scope, might occur when e.g. finishResponse() will fail
    }

    /** Executes the given task with proper error handling and returns next task to execute as future */
    private fun executeNextTask(): CompletableFuture<InputStream> {
        val task = tasks.poll()
        if (errored && task.stage.haltsOnError) {
            queueNextTaskOrFinish() // each subsequent task for this stage will be queued and skipped
            return completedFuture(previousResult)
        }
        val wasAsync = ctx.isAsync() // necessary to detect if user called startAsync() manually
        try {
            /** run code added through submitTask in [JavalinServlet]. This mutates [ctx] */
            task.handler(this)
        } catch (exception: Exception) {
            errored = true
            ctx.resultReference.getAndSet(Result(previousResult)).future.cancel(true)
            exceptionMapper.handle(exception, ctx)
        }
        return ctx.resultReference.getAndSet(Result(previousResult))
            .let { result ->
                when { // we need to check if the user has called startAsync manually, and keep the connection open if so
                    ctx.isAsync() && !wasAsync -> result.copy(future = CompletableFuture<Void>()) // GH-1560: freeze JavalinServletHandler infinitely, TODO: Remove it in Javalin 5.x
                    else -> result
                }
            }
            .also { result -> if (!ctx.isAsync() && !result.future.isDone) startAsyncAndAddDefaultTimeoutListeners() } // start async context only if the future is not already completed
            .also { result -> if (ctx.isAsync()) ctx.req.asyncContext.addListener(onTimeout = { result.future.cancel(true) }) }
            .let { result ->
                result.future
                    .thenAccept { any -> (result.callback?.accept(any) ?: ctx.contextResolver().defaultFutureCallback(ctx, any)) } // callback after future resolves - modifies ctx result, status, etc
                    .thenApply { ctx.resultStream() ?: previousResult } // set value of future to be resultStream (or previous stream)
                    .exceptionally { throwable -> exceptionMapper.handleFutureException(ctx, throwable) } // standard exception handler
                    .thenApply { inputStream -> inputStream.also { queueNextTaskOrFinish() } } // we have to attach the "also" to the input stream to avoid mapping a void
            }
    }

    private fun startAsyncAndAddDefaultTimeoutListeners() = ctx.req.startAsync()
        .addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually
            currentTaskFuture.cancel(true) // cancel current task
            ctx.status(500).result("Request timed out") // default error handling
            errorMapper.handle(ctx.status(), ctx) // user defined error handling
            finishResponse() // write response
        })
        .also { asyncCtx -> asyncCtx.timeout = config.asyncRequestTimeout }

    /** Writes response to the client and frees resources */
    private fun finishResponse() {
        if (finished.getAndSet(true)) return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        try {
            JavalinResponseWrapper(ctx, config, requestType).write(ctx.resultStream())
            config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(ctx.res, throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.isAsync()) ctx.req.asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}

/** Checks if request is executed asynchronously */
private fun Context.isAsync(): Boolean = req.isAsyncStarted

internal fun AsyncContext.addListener(
    onComplete: (AsyncEvent) -> Unit = {},
    onError: (AsyncEvent) -> Unit = {},
    onStartAsync: (AsyncEvent) -> Unit = {},
    onTimeout: (AsyncEvent) -> Unit = {},
) : AsyncContext = apply {
    addListener(object : AsyncListener {
        override fun onComplete(event: AsyncEvent) = onComplete(event)
        override fun onError(event: AsyncEvent) = onError(event)
        override fun onStartAsync(event: AsyncEvent) = onStartAsync(event)
        override fun onTimeout(event: AsyncEvent) = onTimeout(event)
    })
}
