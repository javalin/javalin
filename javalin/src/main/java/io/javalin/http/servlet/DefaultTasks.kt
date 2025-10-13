package io.javalin.http.servlet

import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.util.MethodNotAllowedUtil
import io.javalin.router.Endpoint
import io.javalin.router.EndpointNotFound
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.util.Util.firstOrNull
import io.javalin.util.javalinLazy

object DefaultTasks {

    val BEFORE = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipIfExceptionOccurred = true) { entry.handle(ctx, requestUri) })
        }
    }

    val BEFORE_MATCHED = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val customMethod = if (ctx.method() == HandlerType.INVALID) ctx.req().method else null
        val httpHandlerOrNull by javalinLazy {
            servlet.router.findHttpHandlerEntries(ctx.method(), requestUri, customMethod).firstOrNull()
        }
        val isResourceHandler = httpHandlerOrNull == null && ctx.method() in setOf(HEAD, GET)
        val matchedRouteRoles by javalinLazy { servlet.matchedRoles(ctx, requestUri, customMethod) }
        val resourceRouteRoles by javalinLazy { servlet.cfg.pvt.resourceHandler?.getResourceRouteRoles(ctx) ?: emptySet() }
        val willMatch by javalinLazy {
            ctx.setRouteRoles(if (isResourceHandler) resourceRouteRoles else matchedRouteRoles)
            servlet.willMatch(ctx, requestUri)
        }
        servlet.router.findHttpHandlerEntries(HandlerType.BEFORE_MATCHED, requestUri).forEach { entry ->
            if (willMatch) {
                submitTask(LAST, Task(skipIfExceptionOccurred = true) {
                    val httpHandler = httpHandlerOrNull
                    if (httpHandler != null && !entry.endpoint.hasPathParams()) {
                        entry.endpoint.handle(ctx.update(httpHandler, requestUri))
                    } else {
                        entry.handle(ctx, requestUri)
                    }
                })
            }
        }
    }

    val HTTP = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val customMethod = if (ctx.method() == HandlerType.INVALID) ctx.req().method else null
        val handler = servlet.router.findHttpHandlerEntries(ctx.method(), requestUri, customMethod).firstOrNull()
        
        if (handler != null) {
            submitTask(
                LAST,
                Task {
                    ctx.setRouteRoles(servlet.matchedRoles(ctx, requestUri, customMethod))
                    handler.handle(ctx, requestUri)
                }
            )
            return@TaskInitializer
        }
        
        submitTask(LAST, Task {
            if (ctx.method() == HEAD && servlet.router.hasHttpHandlerEntry(GET, requestUri)) { // return 200, there is a get handler
                return@Task
            }
            if (ctx.method() == HEAD || ctx.method() == GET) { // check for static resources (will write response if found)
                if (servlet.cfg.pvt.resourceHandler?.handle(ctx) == true) return@Task
                if (servlet.cfg.pvt.singlePageHandler.handle(ctx)) return@Task
            }
            if (ctx.handlerType() == HandlerType.BEFORE) { // no match, status will be 404 or 405 after this point
                ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
            }
            val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(servlet.router, requestUri)
            if (servlet.cfg.http.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
            }
            val customMethod = if (ctx.method() == HandlerType.INVALID) ctx.req().method else null
            throw EndpointNotFound(method = ctx.method(), path = requestUri, customMethodName = customMethod)
        })
    }

    val AFTER_MATCHED = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val didMatch by javalinLazy { servlet.willMatch(ctx, requestUri) }
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER_MATCHED, requestUri).forEach { entry ->
            if (didMatch) {
                submitTask(LAST, Task(skipIfExceptionOccurred = false) { entry.handle(ctx, requestUri) })
            }
        }
    }

    val ERROR = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, _ ->
        submitTask(LAST, Task(skipIfExceptionOccurred = false) { servlet.router.handleHttpError(ctx.statusCode(), ctx) })
    }

    val AFTER = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipIfExceptionOccurred = false) { entry.handle(ctx, requestUri) })
        }
    }

    private fun JavalinServlet.willMatch(ctx: JavalinServletContext, requestUri: String) = when {
        ctx.method() == HEAD && this.router.hasHttpHandlerEntry(GET, requestUri) -> true
        else -> {
            val customMethod = if (ctx.method() == HandlerType.INVALID) ctx.req().method else null
            this.router.findHttpHandlerEntries(ctx.method(), requestUri, customMethod).firstOrNull() != null
                || this.cfg.pvt.resourceHandler?.canHandle(ctx) == true
                || this.cfg.pvt.singlePageHandler.canHandle(ctx)
        }
    }

    private fun JavalinServlet.matchedRoles(ctx: JavalinServletContext, requestUri: String, customMethod: String? = null): Set<RouteRole> =
        this.router.findHttpHandlerEntries(ctx.method(), requestUri, customMethod).firstOrNull()?.endpoint?.metadata(Roles::class.java)?.roles ?: emptySet()

}

internal fun Endpoint.hasPathParams() = this.path.contains("{") || this.path.contains("<")
