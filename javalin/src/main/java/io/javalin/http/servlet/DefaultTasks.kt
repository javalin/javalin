package io.javalin.http.servlet

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.NotFoundResponse
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.util.MethodNotAllowedUtil
import io.javalin.security.AccessManagerState.INVOKED
import io.javalin.security.AccessManagerState.NOT_USED
import io.javalin.security.AccessManagerState.PASSED
import io.javalin.security.accessManagerNotConfiguredException
import jakarta.servlet.http.HttpServletResponseWrapper

object DefaultTasks {

    val BEFORE = TaskInitializer {
        it.forMatchedEntries(HandlerType.BEFORE) { entry ->
            it.submitTask(LAST, Task(skipIfExceptionOccurred = true) { entry.handle(it.ctx, it.requestUri) })
        }
    }

    val ACCESS_MANAGER = TaskInitializer<JavalinServletContext> {
        it.servlet.cfg.pvt.accessManager?.also { accessManager ->
            it.forMatchedEntries(it.ctx.method()) { entry ->
                it.submitTask(LAST, Task {
                    it.ctx.update(entry, it.requestUri)
                    it.accessManagerState = INVOKED
                    accessManager.manage(
                        handler = { _ -> it.accessManagerState = PASSED }, // we wrap the handler with [submitTask] to treat it as a separate task
                        ctx = it.ctx,
                        routeRoles = entry.roles
                    )
                })
                return@TaskInitializer
            }
        }
    }

    val HTTP = TaskInitializer<JavalinServletContext> {
        it.forMatchedEntries(it.ctx.method()) { entry ->
            it.submitTask(LAST, Task {
                when {
                    it.accessManagerState == NOT_USED && entry.roles.isNotEmpty() -> throw accessManagerNotConfiguredException()
                    it.accessManagerState == NOT_USED || it.accessManagerState == PASSED -> entry.handle(it.ctx, it.requestUri)
                    else -> { /* do nothing */ }
                }
            })
            return@TaskInitializer
        }
        it.submitTask(LAST, Task {
            if (it.ctx.method() == HEAD && it.matchedHandlers.any { handler -> handler.type == GET }) { // return 200, there is a get handler
                return@Task
            }
            if (it.ctx.method() == HEAD || it.ctx.method() == GET) { // check for static resources (will write response if found)
                if (it.servlet.cfg.pvt.resourceHandler?.handle(it.ctx.req(), JavalinResourceResponseWrapper(it.ctx)) == true) return@Task
                if (it.servlet.cfg.pvt.singlePageHandler.handle(it.ctx)) return@Task
            }
            if (it.ctx.handlerType() == HandlerType.BEFORE) { // no match, status will be 404 or 405 after this point
                it.ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
            }
            val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(it.servlet.matcher, it.requestUri)
            if (it.servlet.cfg.http.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(it.ctx, availableHandlerTypes))
            }
            throw NotFoundResponse()
        })
    }

    val ERROR = TaskInitializer<JavalinServletContext> {
        it.submitTask(LAST, Task(skipIfExceptionOccurred = false) {
            it.servlet.errorMapper.handle(it.ctx.statusCode(), it.ctx)
        })
    }

    val AFTER = TaskInitializer {
        it.forMatchedEntries(HandlerType.AFTER) { entry ->
            it.submitTask(LAST, Task(skipIfExceptionOccurred = false) {
                entry.handle(it.ctx, it.requestUri)
            })
        }
    }

}

private class JavalinResourceResponseWrapper(private val ctx: Context) : HttpServletResponseWrapper(ctx.res()) {
    override fun getOutputStream() = ctx.outputStream()
}
