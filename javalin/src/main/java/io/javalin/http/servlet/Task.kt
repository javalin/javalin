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
    val skipOnExceptionAndRedirect: Boolean = true, // skipped when exception occurs or redirect called from before/beforeMatched
    val handler: TaskHandler<Unit>
)

fun interface TaskHandler<R> {
    fun handle(): R
}
