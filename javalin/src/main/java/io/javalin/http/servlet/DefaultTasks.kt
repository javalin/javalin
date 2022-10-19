package io.javalin.http.servlet

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.NotFoundResponse
import io.javalin.http.servlet.SubmitOrder.FIRST
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.util.MethodNotAllowedUtil
import io.javalin.security.accessManagerNotConfiguredException
import jakarta.servlet.http.HttpServletResponseWrapper

object DefaultTasks {

    val BEFORE = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipIfExceptionOccurred = true) { entry.handle(ctx, requestUri) })
        }
    }

    val HTTP = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.matcher.findEntries(ctx.method(), requestUri).firstOrNull { entry ->
            submitTask(
                LAST,
                Task {
                    when {
                        servlet.cfg.pvt.accessManager != null -> {
                            ctx.update(entry, requestUri)
                            servlet.cfg.pvt.accessManager?.manage(
                                handler = { submitTask(FIRST, Task { entry.handle(ctx, requestUri) }) }, // we wrap the handler with [submitTask] to treat it as a separate task
                                ctx = ctx,
                                routeRoles = entry.roles
                            )
                        }
                        entry.roles.isNotEmpty() -> throw accessManagerNotConfiguredException()
                        else -> entry.handle(ctx, requestUri)
                    }
                }
            )
            return@TaskInitializer
        }
        submitTask(LAST, Task {
            if (ctx.method() == HEAD && servlet.matcher.hasEntries(GET, requestUri)) { // return 200, there is a get handler
                return@Task
            }
            if (ctx.method() == HEAD || ctx.method() == GET) { // check for static resources (will write response if found)
                if (servlet.cfg.pvt.resourceHandler?.handle(ctx.req(), JavalinResourceResponseWrapper(ctx)) == true) return@Task
                if (servlet.cfg.pvt.singlePageHandler.handle(ctx)) return@Task
            }
            if (ctx.handlerType() == HandlerType.BEFORE) { // no match, status will be 404 or 405 after this point
                ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
            }
            val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(servlet.matcher, requestUri)
            if (servlet.cfg.http.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
            }
            throw NotFoundResponse()
        })
    }

    val ERROR = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, _ ->
        submitTask(LAST, Task(skipIfExceptionOccurred = false) { servlet.errorMapper.handle(ctx.statusCode(), ctx) })
    }

    val AFTER = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipIfExceptionOccurred = false) { entry.handle(ctx, requestUri) })
        }
    }

}

private class JavalinResourceResponseWrapper(private val ctx: Context) : HttpServletResponseWrapper(ctx.res()) {
    override fun getOutputStream() = ctx.outputStream()
}
