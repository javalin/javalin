package io.javalin.http.servlet

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint

enum class SubmitOrder {
    FIRST,
    LAST
}

fun interface TaskInitializer<CTX : Context> {
    fun createTasks(submitTask: (SubmitOrder, Task) -> Unit, servlet: JavalinServlet, ctx: CTX, requestUri: String)
}

class Task(
    val label: String = "task", // what this task does, e.g. "before /*" or "GET /users/{id}"
    val skipOnExceptionAndRedirect: Boolean = true, // skipped when exception occurs or redirect called from before/beforeMatched
    val handler: TaskHandler<Unit>
)

fun interface TaskHandler<R> {
    fun handle(): R
}

/**
 * Observes each task in the request pipeline as it completes. Registered via
 * [io.javalin.config.JavalinState.servletTaskObservers] - register observers before the server starts.
 *
 * Observers are invoked on the request thread (and, for async requests, the future-completion thread), so an
 * observer must be thread-safe: concurrent requests call it in parallel. [throwable] is non-null if the task
 * threw. [durationNanos] is the task's synchronous execution time, not end-to-end request latency (an async
 * handler returns as soon as it hands off its future). An exception thrown by an observer is caught and logged,
 * so it never affects the request.
 */
fun interface TaskObserver {
    fun onTaskCompleted(ctx: Context, task: Task, durationNanos: Long, throwable: Throwable?)
}

// Canonical [Task.label] values, shared by the producers (DefaultTasks/JavalinServlet) and any consumer
// (e.g. the DevTools plugin maps handler labels back to source locations), so the two can't drift.
internal const val NO_MATCH_LABEL = "no-match (static/404/405)"
internal const val ERROR_MAPPING_LABEL = "error-mapping"
internal const val EXCEPTION_LABEL_PREFIX = "exception:"

/** The [Task.label] for a handler entry, e.g. a before-handler on `/`, or `GET /users/{id}`. */
internal fun labelName(entry: ParsedEndpoint): String = labelName(entry.endpoint.method, entry.endpoint.path)

/** Primitive form for callers that only have a method + path (e.g. the handlerAdded event's HandlerMetaInfo). */
internal fun labelName(type: HandlerType, path: String): String = when (type) {
    HandlerType.BEFORE -> "before $path"
    HandlerType.BEFORE_MATCHED -> "beforeMatched $path"
    HandlerType.AFTER_MATCHED -> "afterMatched $path"
    HandlerType.AFTER -> "after $path"
    else -> "${type.name} $path"
}

/** The [Task.label] for the task that handles a thrown exception, e.g. "exception:IllegalStateException". */
internal fun exceptionLabel(throwable: Throwable): String = "$EXCEPTION_LABEL_PREFIX${throwable.javaClass.simpleName}"
