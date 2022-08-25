package io.javalin.http

import io.javalin.config.JavalinConfig
import io.javalin.http.util.ETagGenerator
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

interface StageName
enum class DefaultName : StageName { BEFORE, HTTP, ERROR, AFTER }

data class Stage(
    val name: StageName,
    val skipTasksOnException: Boolean, // tasks in this stage can be aborted by throwing an exception
    val initializer: StageInitializer = {} // DSL method to add task to the stage's queue
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
 * The lifecycle consists of multiple lifecycle stages (before/http/etc), each of which
 * can have one or more [remainingTasks]. The default lifecycle is defined in [JavalinServlet].
 * [JavalinServletHandler] is called only once per request, and has a mutable state.
 */
class JavalinServletHandler(
    lifecycleStages: ArrayDeque<Stage>,
    private val cfg: JavalinConfig,
    private val errorMapper: ErrorMapper,
    private val exceptionMapper: ExceptionMapper,
    val ctx: DefaultContext,
    val requestUri: String = ctx.path().removePrefix(ctx.contextPath()),
) {

    /** Queue of tasks to execute within the current [Stage] */
    private val remainingTasks = ArrayDeque<Task>(4)

    init {
        lifecycleStages.forEach { stage ->
            stage.initializer(this) { handler -> remainingTasks.offer(Task(stage, handler)) }
        }
    }

    /** Indicates if exception occurred during execution of a tasks chain */
    private var exceptionOccurred = false

    /** Indicates if [JavalinServletHandler] already wrote response to client, requires support for atomic switch */
    private val responseWritten = AtomicBoolean(false)

    /**
     * This method starts execution process of all stages in a given lifecycle.
     * Execution is based on recursive calls of this method,
     * because we need a lazy evaluation of next tasks in a chain to support multiple concurrent stages.
     */
    internal fun nextTaskOrFinish() {
        val nextTask = remainingTasks.poll() ?: return writeResponseAndLog() // terminate if we're out of tasks
        if (exceptionOccurred && nextTask.stage.skipTasksOnException) return nextTaskOrFinish() // skip this stage's tasks
        try {
            nextTask.handler(this)
        } catch (throwable: Throwable) {
            handleUserCodeThrowable(throwable)
        }
        val userFuture = ctx.consumeUserFuture() ?: return nextTaskOrFinish() // if there is no user future, we immediately move on to the next task
        userFuture // there is a user future! attach error handling, callback, and start async
            .exceptionally { throwable -> handleUserCodeThrowable(throwable) }
            .whenComplete { _, _ -> nextTaskOrFinish() }
            .also { if (!ctx.req().isAsyncStarted) startAsyncAndAddDefaultTimeoutListeners() }
            .also { ctx.req().asyncContext.addListener(onTimeout = { userFuture.cancel(true) }) }
    }

    private fun handleUserCodeThrowable(throwable: Throwable): Nothing? {
        exceptionOccurred = true
        when (throwable) {
            is Exception -> exceptionMapper.handle(throwable, ctx)
            else -> exceptionMapper.handleUnexpectedThrowable(throwable, ctx.res())
        }
        return null  // for easy chaining in exceptionally()
    }

    private fun startAsyncAndAddDefaultTimeoutListeners() = ctx.req().startAsync().apply {
        timeout = cfg.http.asyncTimeout
        addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually + it's not thread-safe
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR) // default error handling
            errorMapper.handle(ctx.statusCode(), ctx) // user defined error handling
            if (ctx.resultStream() == null) ctx.result("Request timed out") // write default response only if handler didn't do anything
            writeResponseAndLog() // write response
        })
    }

    /** Writes response to the client and frees resources */
    private fun writeResponseAndLog() {
        // prevent writing more than once (ex. both async requests+errors).
        // it's required because timeout listener can terminate the flow at any time.
        if (responseWritten.getAndSet(true)) return
        try {
            ctx.outputStream().use { outputStream ->
                ctx.resultStream()?.use { resultStream ->
                    val etagWritten = ETagGenerator.tryWriteEtagAndClose(cfg.http.generateEtags, ctx, resultStream)
                    if (!etagWritten) resultStream.copyTo(outputStream)
                }
            }
            cfg.pvt.requestLogger?.handle(ctx, ctx.executionTimeMs())
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(throwable, ctx.res()) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.req().isAsyncStarted) ctx.req().asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}

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
