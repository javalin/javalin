package io.javalin.http.servlet

import io.javalin.http.Context

fun interface TaskInitializer<C : Context> {
    fun createTasks(
        submitTask: (Task) -> Unit,
        servlet: JavalinServlet,
        ctx: C,
        requestUri: String,
    )
}

data class Task(
    val skipIfErrorOccurred: Boolean = true, // tasks in this stage can be aborted by throwing an exception
    val handler: TaskHandler<Unit>
)

fun interface TaskHandler<R> {
    fun handle(): R
}
