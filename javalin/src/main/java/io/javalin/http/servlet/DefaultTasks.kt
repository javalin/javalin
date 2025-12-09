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
            // BEFORE handlers can be skipped if an exception occurs or if Context#redirect is called
            submitTask(LAST, Task(skipIfExceptionOccurred = true) { entry.handle(ctx, requestUri) })
        }
    }

    val BEFORE_MATCHED = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val httpHandlerOrNull by javalinLazy {
            servlet.router.findHttpHandlerEntries(ctx.method(), requestUri).firstOrNull()
        }
        val isResourceHandler = httpHandlerOrNull == null && ctx.method() in setOf(HEAD, GET)
        val matchedRouteRoles by javalinLazy { servlet.matchedRoles(ctx, requestUri) }
        val resourceRouteRoles by javalinLazy { servlet.cfg.resourceHandler?.resourceRouteRoles(ctx) ?: emptySet() }
        val willMatch by javalinLazy {
            ctx.setRouteRoles(if (isResourceHandler) resourceRouteRoles else matchedRouteRoles)
            servlet.willMatch(ctx, requestUri)
        }

        servlet.router.findHttpHandlerEntries(HandlerType.BEFORE_MATCHED, requestUri).forEach { entry ->
            if (willMatch) {
                // BEFORE_MATCHED handlers can be skipped if an exception occurs or if Context#redirect is called from a BEFORE handler
                submitTask(LAST, Task(skipIfExceptionOccurred = true) {
                    entry.handle(ctx, requestUri)
                })
            }
        }
    }

    val HTTP = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(ctx.method(), requestUri).firstOrNull { entry ->
            submitTask(
                LAST,
                // HTTP handler (the matched endpoint) cannot be skipped by exceptions but can be skipped by Context#redirect from BEFORE
                Task {
                    ctx.setRouteRoles(servlet.matchedRoles(ctx, requestUri)) // set roles for the matched handler
                    entry.handle(ctx, requestUri)
                }
            )
            return@TaskInitializer
        }
        submitTask(LAST, Task {
            if (ctx.method() == HEAD && servlet.router.hasHttpHandlerEntry(GET, requestUri)) { // return 200, there is a get handler
                return@Task
            }
            if (ctx.method() == HEAD || ctx.method() == GET) { // check for static resources (will write response if found)
                if (servlet.cfg.resourceHandler?.handle(ctx) == true) return@Task
                if (servlet.cfg.singlePageHandler.handle(ctx)) return@Task
            }
            // No match, status will be 404 or 405 after this point
            // The endpoint will still be the placeholder with path ""
            val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(servlet.router, requestUri)
            if (servlet.cfg.http.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.availableHandlerTypes(ctx, availableHandlerTypes))
            }
            throw EndpointNotFound(method = ctx.method(), path = requestUri)
        })
    }

    val AFTER_MATCHED = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val didMatch by javalinLazy { servlet.willMatch(ctx, requestUri) }
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER_MATCHED, requestUri).forEach { entry ->
            if (didMatch) {
                // AFTER_MATCHED handlers always run (even after exceptions or redirects) for cleanup/logging
                submitTask(LAST, Task(skipIfExceptionOccurred = false) { entry.handle(ctx, requestUri) })
            }
        }
    }

    val ERROR = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, _ ->
        // ERROR handlers always run to handle errors, regardless of exceptions
        submitTask(LAST, Task(skipIfExceptionOccurred = false) { servlet.router.handleHttpError(ctx.statusCode(), ctx) })
    }

    val AFTER = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER, requestUri).forEach { entry ->
            // AFTER handlers always run for cleanup/logging, regardless of exceptions or redirects
            submitTask(LAST, Task(skipIfExceptionOccurred = false) { entry.handle(ctx, requestUri) })
        }
    }

    private fun JavalinServlet.willMatch(ctx: JavalinServletContext, requestUri: String) = when {
        ctx.method() == HEAD && this.router.hasHttpHandlerEntry(GET, requestUri) -> true
        this.router.findHttpHandlerEntries(ctx.method(), requestUri).firstOrNull() != null -> true
        this.cfg.resourceHandler?.canHandle(ctx) == true -> true
        this.cfg.singlePageHandler.canHandle(ctx) -> true
        else -> false
    }

    private fun JavalinServlet.matchedRoles(ctx: JavalinServletContext, requestUri: String): Set<RouteRole> =
        this.router.findHttpHandlerEntries(ctx.method(), requestUri).firstOrNull()?.endpoint?.metadata(Roles::class.java)?.roles ?: emptySet()

}

internal fun Endpoint.hasPathParams() = this.path.contains("{") || this.path.contains("<")
