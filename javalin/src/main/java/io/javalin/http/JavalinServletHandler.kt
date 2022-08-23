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
    val haltsOnException: Boolean, // tasks in this stage can be aborted by throwing an exception
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
 * The lifecycle consists of multiple [stages] (before/http/etc), each of which
 * can have one or more [tasks]. The default lifecycle is defined in [JavalinServlet].
 * [JavalinServletHandler] is called only once per request, and has a mutable state.
 */
class JavalinServletHandler(
    private val stages: ArrayDeque<Stage>,
    private val cfg: JavalinConfig,
    private val errorMapper: ErrorMapper,
    private val exceptionMapper: ExceptionMapper,
    val ctx: DefaultContext,
    val requestUri: String = ctx.path().removePrefix(ctx.contextPath()),
) {

    /** Queue of tasks to execute within the current [Stage] */
    private val tasks = ArrayDeque<Task>(4)

    /** Indicates if exception occurred during execution of a tasks chain */
    private var exceptionOccurred = false

    /** Indicates if [JavalinServletHandler] already wrote response to client, requires support for atomic switch */
    private val responseWritten = AtomicBoolean(false)

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
        if (tasks.isEmpty()) {
            writeResponseAndLog()
            return
        }
        val nextTask = tasks.poll().takeUnless { exceptionOccurred && it.stage.haltsOnException } // each subsequent task for this stage will be queued and skipped
        try {
            nextTask?.handler?.invoke(this)
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
        val future = this.ctx.userFuture()
        if (future == null) {
            queueNextTaskOrFinish()
        } else {
            future.thenAccept { queueNextTaskOrFinish() }
        }
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
            exceptionMapper.handleUnexpectedThrowable(ctx.res(), throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.isAsync()) ctx.req().asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}

/** Checks if request is executed asynchronously */
private fun Context.isAsync(): Boolean = req().isAsyncStarted

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
