/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.security.RouteRole
import io.javalin.core.util.CorsPlugin
import io.javalin.core.util.LogUtil
import io.javalin.http.HandlerType.AFTER
import io.javalin.http.HandlerType.BEFORE
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MethodNotAllowedUtil
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()
    private val lifecycle = mutableListOf<Stage>()

    init {
        addLifecycleStages(
            Stage("before") { submitTask ->
                matcher.findEntries(BEFORE, requestUri).forEach { entry ->
                    submitTask { handle(ctx, requestUri, entry) }
                }
            },
            Stage("http") { submitTask ->
                matcher.findEntries(type, requestUri).firstOrNull()?.let { entry ->
                    submitTask { handle(ctx, requestUri, entry) }
                    return@Stage // return after first match
                }
                submitTask {
                    if (type == HEAD && matcher.hasEntries(GET, requestUri)) { // return 200, there is a get handler
                        return@submitTask
                    }
                    if (type == HEAD || type == GET) { // check for static resources (will write response if found)
                        if (config.inner.resourceHandler?.handle(it.request, JavalinResponseWrapper(it.response, responseWrapperContext)) == true) return@submitTask
                        if (config.inner.singlePageHandler.handle(ctx)) return@submitTask
                    }
                    if (type == OPTIONS && config.inner.plugins[CorsPlugin::class.java] != null) { // CORS is enabled, so we return 200 for OPTIONS
                        return@submitTask
                    }
                    if (ctx.handlerType == BEFORE) { // no match, status will be 404 or 405 after this point
                        ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
                    }
                    val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(matcher, requestUri)
                    if (config.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                        throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
                    }
                    throw NotFoundResponse()
                }
            },
            Stage("error", ignoresExceptions = true) { submitTask ->
                submitTask { handleError(ctx) }
            },
            Stage("after", ignoresExceptions = true) { submitTask ->
                matcher.findEntries(AFTER, requestUri).forEach { entry ->
                    submitTask { handle(ctx, requestUri, entry) }
                }
            }
        )
    }

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val ctx = Context(request, response, config.inner.appAttributes)
            LogUtil.setup(ctx, matcher, config.inner.requestLogger != null)
            ctx.contentType(config.defaultContentType)

            JavalinServletHandler(
                lifecycle = lifecycle,
                servlet = this,
                ctx = ctx,
                type = HandlerType.fromServletRequest(request),
                requestUri = request.requestURI.removePrefix(request.contextPath),
                responseWrapperContext = ResponseWrapperContext(request, config),
                request = request,
                response = response
            ).execute()
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable)
        }
    }

    internal fun handle(ctx: Context, requestUri: String, handlerEntry: HandlerEntry) =
        handlerEntry.handler.handle(ContextUtil.update(ctx, handlerEntry, requestUri))

    internal fun handleError(ctx: Context) =
        errorMapper.handle(ctx.status(), ctx)

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<RouteRole>) {
        val protectedHandler = if (handlerType.isHttpMethod()) Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, config.ignoreTrailingSlashes, protectedHandler, handler))
    }

    fun addLifecycleStages(vararg stages: Stage, stageBefore: String? = null) {
        if (stageBefore == null)
            lifecycle.addAll(stages)
        else
            lifecycle.addAll(lifecycle.indexOfFirst { it.name == stageBefore }, stages.toList())
    }

    fun removeLifecycleStage(name: String): Boolean =
        lifecycle.removeIf { it.name == name }

}
