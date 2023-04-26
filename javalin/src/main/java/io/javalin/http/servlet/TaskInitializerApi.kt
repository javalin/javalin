package io.javalin.http.servlet

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.servlet.SubmitOrder.FIRST
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.routing.HandlerEntry
import io.javalin.security.AccessManagerState

fun interface TaskInitializer<CTX : Context> {
    fun createTasks(initializerContext: TaskInitializerContext<CTX>)
}

data class Task(
    val skipIfExceptionOccurred: Boolean = true, // tasks in this stage can be aborted by throwing an exception
    val handler: TaskHandler<Unit>
)

fun interface TaskHandler<R> {
    fun handle(): R
}

enum class SubmitOrder {
    FIRST,
    LAST
}

interface TaskInitializerContext<CTX : Context> {
    val servlet: JavalinServlet
    val ctx: CTX
    val requestUri: String
    val matchedHandlers: List<HandlerEntry>
    var accessManagerState: AccessManagerState

    fun submitTask(order: SubmitOrder, task: Task)
}

inline fun TaskInitializerContext<*>.forMatchedEntries(handlerType: HandlerType, block: (HandlerEntry) -> Unit) {
    for (matchedHandler in matchedHandlers) {
        if (matchedHandler.type == handlerType) {
            block(matchedHandler)
        }
    }
}

class JavalinTaskInitializerContext(
    override val servlet: JavalinServlet,
    override val ctx: JavalinServletContext,
    override val requestUri: String,
) : TaskInitializerContext<JavalinServletContext> {

    override var accessManagerState: AccessManagerState = AccessManagerState.NOT_USED
    override val matchedHandlers: List<HandlerEntry> = servlet.matcher.findEntries(requestUri)

    override fun submitTask(order: SubmitOrder, task: Task) {
        when (order) {
            FIRST -> ctx.tasks.offerFirst(task)
            LAST -> ctx.tasks.add(task)
        }
    }

}
