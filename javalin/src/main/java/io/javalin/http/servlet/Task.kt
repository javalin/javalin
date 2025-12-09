package io.javalin.http.servlet

import io.javalin.http.Context

enum class SubmitOrder {
    FIRST,
    LAST
}

fun interface TaskInitializer<CTX : Context> {
    fun createTasks(submitTask: (SubmitOrder, Task) -> Unit, servlet: JavalinServlet, ctx: CTX, requestUri: String)
}

data class Task(
    /**
     * Determines if this task should be skipped when an exception has occurred during request processing.
     *
     * This flag serves two purposes:
     * 1. Exception handling: When an exception is thrown, tasks with skipIfExceptionOccurred=true are skipped
     *    to avoid executing handlers that depend on successful prior execution.
     * 2. Early termination: Used by Context#redirect when called from a BEFORE handler to skip the HTTP handler
     *    and any remaining BEFORE/BEFORE_MATCHED handlers.
     *
     * Tasks that should always run (like error handlers and after handlers) have skipIfExceptionOccurred=false.
     */
    val skipIfExceptionOccurred: Boolean = true,
    val handler: TaskHandler<Unit>
)

fun interface TaskHandler<R> {
    fun handle(): R
}
