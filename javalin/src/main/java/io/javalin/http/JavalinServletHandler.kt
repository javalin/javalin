package io.javalin.http

import io.javalin.config.JavalinConfig
import io.javalin.config.contextResolver
import io.javalin.util.LogUtil
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.io.InputStream
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.failedFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

interface StageName
enum class DefaultName : StageName { BEFORE, HTTP, ERROR, AFTER }

private object FutureCallbackStage : StageName {
    val stage = Stage(this, haltsOnError = false)
}

data class Stage(
    val name: StageName,
    val haltsOnError: Boolean = true, // tasks in this scope should be executed even if some previous stage ended up with exception
    val initializer: StageInitializer = {} // DSL method to add task to the stage's queue
)

internal data class Result<VALUE : Any?>(
    val previous: InputStream? = null,
    val future: CompletableFuture<VALUE>? = null,
    val launch: Runnable? = null,
    val callback: Consumer<VALUE>? = null,
)

internal data class Task(
    val stage: Stage,
    val handler: TaskHandler
)

internal data class ExecutionResult(
    val result: Result<out Any?>,
    val value: Any?
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
    private val cfg: JavalinConfig,
    private val errorMapper: ErrorMapper,
    private val exceptionMapper: ExceptionMapper,
    val ctx: Context,
    val requestType: HandlerType = HandlerType.fromServletRequest(ctx.req),
    val requestUri: String = ctx.req.requestURI.removePrefix(ctx.req.contextPath),
) {

    /** Queue of tasks to execute within the current [Stage] */
    private val tasks = ArrayDeque<Task>(4)

    /** Future representing currently queued task */
    private var currentTaskFuture: CompletableFuture<*> = completedFuture(null)

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
                .thenCompose { executeNextTask() } // chain next task into current future
                .thenApply { queueNextTaskOrFinish() } // continue standard execution of queue
                .exceptionally { exceptionMapper.handleUnexpectedThrowable(ctx.res, it) } // default catch-all for whole scope, might occur when e.g. finishResponse() will fail
    }

    /**
     * Executes current task from queue.
     * Each phase (user's handlers) of task execution supports proper result handling by chaining it with [handleFutureResultReference].
     */
    private fun executeNextTask(): CompletableFuture<*> =
        tasks.poll()
            .takeUnless { errored && it.stage.haltsOnError } // each subsequent task for this stage will be queued and skipped
            ?.let { handleFutureResultReference { it.handler(this) } } // execute main task
            ?.exceptionallyComposeFallback { throwable -> // handle exception from task with exception mapper
                handleFutureResultReference {
                    errored = true
                    exceptionMapper.handleFutureException(ctx, throwable)
                }
            }
            ?.thenAccept { if (it.result.future != null) tasks.offerFirst(createFutureCallbackTask(it)) } // add future callback add the beginning of queue
            ?: completedFuture(null) // stub future if task was skipped

    @Suppress("UNCHECKED_CAST")
    private fun createFutureCallbackTask(executionResult: ExecutionResult): Task =
        Task(
            stage = FutureCallbackStage.stage,
            handler = {
                executionResult.result.callback
                    ?.let { (it as Consumer<Any?>).accept(executionResult.value) }
                    ?: ctx.contextResolver().defaultFutureCallback(ctx, executionResult.value)
            }
        )

    /** Handles futures provided by user through ctx.future() in various handlers */
    private fun handleFutureResultReference(handler: () -> Unit): CompletableFuture<ExecutionResult> {
        val executedTask = runCatching { handler() }

        val result = ctx.resultReference.getAndUpdate { Result(ctx.resultStream() ?: it.previous) } // remove result to process from context
        result.launch?.run()

        if (!ctx.isAsync() && result.future?.isDone == false) {
            startAsyncAndAddDefaultTimeoutListeners() // starts async context only if future is not already completed
        }
        if (ctx.isAsync() && result.future?.isDone == false) {
            ctx.req.asyncContext.addListener(onTimeout = { result.future.cancel(true) }) // registers timeout listener only if future is not already completed
        }

        return executedTask
            .fold(
                onSuccess = { result.future?.thenApply { ExecutionResult(result, it) } }, // map result to ExecutionResult for further consumers
                onFailure = { failedFuture(it) } // or wrap exception with future
            )
            ?.exceptionally {
                result.future?.cancel(true) // cancel user's future in case of exception
                throw it // rethrow origin exception
            }
            ?: completedFuture(ExecutionResult(Result(ctx.resultStream()), null)) // default result in case of lack of user's future
    }

    private fun startAsyncAndAddDefaultTimeoutListeners() = ctx.req.startAsync()
        .addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually + it's not thread-safe
            ctx.resultReference.getAndSet(Result()).also { // cleanup current state of ctx, timeout listener will override it
                it.future?.cancel(true)
                it.previous?.close()
            }
            currentTaskFuture.cancel(true) // cancel current task
            ctx.status(500) // default error handling
            errorMapper.handle(ctx.status(), ctx) // user defined error handling
            if (ctx.resultStream() == null) ctx.result("Request timed out") // write default response only if handler didn't do anything
            finishResponse() // write response
        })
        .also { asyncCtx -> asyncCtx.timeout = cfg.http.asyncTimeout }

    /** Writes response to the client and frees resources */
    private fun finishResponse() {
        if (finished.getAndSet(true)) return // prevent writing more than once (ex. both async requests+errors) [it's required because timeout listener can terminate the flow at any tim]
        try {
            JavalinResponseWrapper(ctx, cfg, requestType).write(ctx.resultStream())
            cfg.pvt.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(ctx.res, throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.isAsync()) ctx.req.asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}

/** [CompletableFuture.exceptionallyCompose] method is available since JDK12+, so we need a fallback for JDK11 */
fun <T> CompletableFuture<T>.exceptionallyComposeFallback(mapping: (Throwable) -> CompletionStage<T>): CompletableFuture<T> =
    thenApply { completedFuture(it) as CompletionStage<T> }
        .exceptionally { mapping(it) }
        .thenCompose { it }

/** Checks if request is executed asynchronously */
private fun Context.isAsync(): Boolean = req.isAsyncStarted

internal fun AsyncContext.addListener(
    onComplete: (AsyncEvent) -> Unit = {},
    onError: (AsyncEvent) -> Unit = {},
    onStartAsync: (AsyncEvent) -> Unit = {},
    onTimeout: (AsyncEvent) -> Unit = {},
): AsyncContext = apply {
    addListener(object : AsyncListener {
        override fun onComplete(event: AsyncEvent) = onComplete(event)
        override fun onError(event: AsyncEvent) = onError(event)
        override fun onStartAsync(event: AsyncEvent) = onStartAsync(event)
        override fun onTimeout(event: AsyncEvent) = onTimeout(event)
    })
}
