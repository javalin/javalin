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
import io.javalin.http.util.MethodNotAllowedUtil
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        try {
            handle(req, res)
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(res, throwable)
        }
    }

    private fun handle(request: HttpServletRequest, response: HttpServletResponse) {
        val type = HandlerType.fromServletRequest(request)
        val responseWrapperContext = ResponseWrapperContext(request, config)
        val requestUri = request.requestURI.removePrefix(request.contextPath)
        val ctx = Context(request, response, config.inner.appAttributes)
        val context = JavalinFlowContext(type, responseWrapperContext, requestUri, ctx)

        val beforeScope = Scope("before") {
            matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
                submit(it) { handle(context, entry) }
            }
        }
        val httpScope = Scope("http") {
            matcher.findEntries(type, requestUri).firstOrNull()?.let { entry ->
                submit(it) { handle(context, entry) }
                return@Scope // return after first match
            }
            submit(it) {
                if (type == HandlerType.HEAD && matcher.hasEntries(HandlerType.GET, requestUri)) { // return 200, there is a get handler
                    return@submit
                }
                if (type == HandlerType.HEAD || type == HandlerType.GET) { // check for static resources (will write response if found)
                    if (config.inner.resourceHandler?.handle(request, JavalinResponseWrapper(response, responseWrapperContext)) == true) return@submit
                    if (config.inner.singlePageHandler.handle(ctx)) return@submit
                }
                if (type == HandlerType.OPTIONS && isCorsEnabled(config)) { // CORS is enabled, so we return 200 for OPTIONS
                    return@submit
                }
                if (ctx.handlerType == HandlerType.BEFORE) { // no match, status will be 404 or 405 after this point
                    ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
                }
                val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(matcher, requestUri)
                if (config.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                    throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
                }
                throw NotFoundResponse()
            }
        }
        val errorScope = Scope("error", allowsErrors = true) {
            submit(it) { handleError(ctx) }
        }
        val afterScope = Scope("after", allowsErrors = true) {
            matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
                submit(it) { handle(context, entry) }
            }
        }

        LogUtil.setup(ctx, matcher, config.inner.requestLogger != null)
        ctx.contentType(config.defaultContentType)
        val flow = JavalinServletFlow(this, context, request, response, listOf(beforeScope, httpScope, errorScope, afterScope))
        flow.start() // Start request lifecycle
    }

    private fun updateContext(servletContext: JavalinFlowContext, handlerEntry: HandlerEntry) = servletContext.ctx.apply {
        matchedPath = handlerEntry.path
        pathParamMap = handlerEntry.extractPathParams(servletContext.requestUri)
        handlerType = handlerEntry.type
        if (handlerType != HandlerType.AFTER) endpointHandlerPath = handlerEntry.path // Idk what it does
    }

    internal fun handle(context: JavalinFlowContext, handlerEntry: HandlerEntry) =
        handlerEntry.handler.handle(updateContext(context, handlerEntry))

    internal fun handleError(ctx: Context) =
        errorMapper.handle(ctx.status(), ctx)

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<RouteRole>) {
        val protectedHandler =
            if (handlerType.isHttpMethod())
                Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) }
            else
                handler

        matcher.add(HandlerEntry(handlerType, path, config.ignoreTrailingSlashes, protectedHandler, handler))
    }

    private fun isCorsEnabled(config: JavalinConfig) =
        config.inner.plugins[CorsPlugin::class.java] != null

}
