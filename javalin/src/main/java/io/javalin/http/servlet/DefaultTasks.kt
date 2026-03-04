package io.javalin.http.servlet

import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.util.MethodNotAllowedUtil
import io.javalin.router.Endpoint
import io.javalin.router.EndpointNotFound
import io.javalin.router.ParsedEndpoint
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.util.javalinLazy

private const val CACHED_HTTP_HANDLER_KEY = "javalin-cached-http-handler"
private const val CACHED_WILL_MATCH_KEY = "javalin-cached-will-match"

/** Wrapper to distinguish a cached null entry from "not yet computed". */
private class CachedEntry(@JvmField val value: ParsedEndpoint?)

object DefaultTasks {

    /**
     * Returns the first matching HTTP handler entry for ctx.method(), caching the result
     * on the request so that BEFORE_MATCHED, HTTP, AFTER_MATCHED, and willMatch share a single lookup.
     */
    private fun cachedHttpHandler(servlet: JavalinServlet, ctx: JavalinServletContext, requestUri: String): ParsedEndpoint? {
        val cached = ctx.req().getAttribute(CACHED_HTTP_HANDLER_KEY)
        if (cached != null) {
            return (cached as CachedEntry).value
        }
        val entry = servlet.router.findHttpHandlerEntries(ctx.method(), requestUri).firstOrNull()
        ctx.req().setAttribute(CACHED_HTTP_HANDLER_KEY, CachedEntry(entry))
        return entry
    }

    private fun cachedWillMatch(servlet: JavalinServlet, ctx: JavalinServletContext, requestUri: String): Boolean {
        val cached = ctx.req().getAttribute(CACHED_WILL_MATCH_KEY)
        if (cached != null) return cached as Boolean
        val result = servlet.willMatch(ctx, requestUri)
        ctx.req().setAttribute(CACHED_WILL_MATCH_KEY, result)
        return result
    }

    val BEFORE = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipOnExceptionAndRedirect = true) { entry.handle(ctx, requestUri) })
        }
    }

    val BEFORE_MATCHED = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val httpHandlerOrNull by javalinLazy { cachedHttpHandler(servlet, ctx, requestUri) }
        val isResourceHandler = httpHandlerOrNull == null && ctx.method() in setOf(HEAD, GET)
        val matchedRouteRoles by javalinLazy { httpHandlerOrNull?.endpoint?.metadata(Roles::class.java)?.roles ?: emptySet() }
        val resourceRouteRoles by javalinLazy { servlet.cfg.resourceHandler?.resourceRouteRoles(ctx) ?: emptySet() }
        val willMatch by javalinLazy {
            ctx.setRouteRoles(if (isResourceHandler) resourceRouteRoles else matchedRouteRoles)
            cachedWillMatch(servlet, ctx, requestUri)
        }

        servlet.router.findHttpHandlerEntries(HandlerType.BEFORE_MATCHED, requestUri).forEach { entry ->
            if (willMatch) {
                httpHandlerOrNull?.let { ctx.endpoints().matchedHttpEndpointInternal = it.endpoint }
                submitTask(LAST, Task(skipOnExceptionAndRedirect = true) {
                    entry.handle(ctx, requestUri)
                })
            }
        }
    }

    val HTTP = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        val entry = cachedHttpHandler(servlet, ctx, requestUri)
        if (entry != null) {
            submitTask(
                LAST,
                Task {
                    val roles = entry.endpoint.metadata(Roles::class.java)?.roles ?: emptySet()
                    ctx.setRouteRoles(roles)
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
        val didMatch by javalinLazy { cachedWillMatch(servlet, ctx, requestUri) }
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER_MATCHED, requestUri).forEach { entry ->
            if (didMatch) {
                submitTask(LAST, Task(skipOnExceptionAndRedirect = false) { entry.handle(ctx, requestUri) })
            }
        }
    }

    val ERROR = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, _ ->
        submitTask(LAST, Task(skipOnExceptionAndRedirect = false) { servlet.router.handleHttpError(ctx.statusCode(), ctx) })
    }

    val AFTER = TaskInitializer<JavalinServletContext> { submitTask, servlet, ctx, requestUri ->
        servlet.router.findHttpHandlerEntries(HandlerType.AFTER, requestUri).forEach { entry ->
            submitTask(LAST, Task(skipOnExceptionAndRedirect = false) { entry.handle(ctx, requestUri) })
        }
    }

    private fun JavalinServlet.willMatch(ctx: JavalinServletContext, requestUri: String) = when {
        ctx.method() == HEAD && this.router.hasHttpHandlerEntry(GET, requestUri) -> true
        cachedHttpHandler(this, ctx, requestUri) != null -> true
        this.cfg.resourceHandler?.canHandle(ctx) == true -> true
        this.cfg.singlePageHandler.canHandle(ctx) -> true
        else -> false
    }

}

internal fun Endpoint.hasPathParams() = this.path.contains("{") || this.path.contains("<")
