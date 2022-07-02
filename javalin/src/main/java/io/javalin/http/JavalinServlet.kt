/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.config.JavalinConfig
import io.javalin.core.security.RouteRole
import io.javalin.plugin.CorsPlugin
import io.javalin.core.util.LogUtil
import io.javalin.http.HandlerType.AFTER
import io.javalin.http.HandlerType.BEFORE
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MethodNotAllowedUtil
import java.util.*
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    /**
     * Default request lifecycle used by servlet to handle HTTP requests.
     * You can modify its state to add/remove stages and directly affect the way that Javalin handles requests.
     */
    val lifecycle = mutableListOf(
        Stage(DefaultName.BEFORE) { submitTask ->
            matcher.findEntries(BEFORE, requestUri).forEach { entry ->
                submitTask { entry.handler.handle(ContextUtil.update(ctx, entry, requestUri)) }
            }
        },
        Stage(DefaultName.HTTP) { submitTask ->
            matcher.findEntries(requestType, requestUri).firstOrNull()?.let { entry ->
                submitTask { entry.handler.handle(ContextUtil.update(ctx, entry, requestUri)) }
                return@Stage // return after first match
            }
            submitTask {
                if (requestType == HEAD && matcher.hasEntries(GET, requestUri)) { // return 200, there is a get handler
                    return@submitTask
                }
                if (requestType == HEAD || requestType == GET) { // check for static resources (will write response if found)
                    if (config.inner.resourceHandler?.handle(it.ctx.req, JavalinResponseWrapper(it.ctx, config, requestType)) == true) return@submitTask
                    if (config.inner.singlePageHandler.handle(ctx)) return@submitTask
                }
                if (requestType == OPTIONS && config.isCorsEnabled()) { // CORS is enabled, so we return 200 for OPTIONS
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
        Stage(DefaultName.ERROR, haltsOnError = false) { submitTask ->
            submitTask { errorMapper.handle(ctx.status(), ctx) }
        },
        Stage(DefaultName.AFTER, haltsOnError = false) { submitTask ->
            matcher.findEntries(AFTER, requestUri).forEach { entry ->
                submitTask { entry.handler.handle(ContextUtil.update(ctx, entry, requestUri)) }
            }
        }
    )

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val ctx = Context(request, response, config.inner.appAttributes)
            LogUtil.setup(ctx, matcher, config.inner.requestLogger != null)
            ctx.contentType(config.defaultContentType)

            JavalinServletHandler(
                stages = ArrayDeque(lifecycle),
                config = config,
                errorMapper = errorMapper,
                exceptionMapper = exceptionMapper,
                ctx = ctx
            ).queueNextTaskOrFinish()
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable)
        }
    }

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<RouteRole>) {
        val protectedHandler = if (handlerType.isHttpMethod()) Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, config.routing, protectedHandler, handler))
    }

    private fun JavalinConfig.isCorsEnabled() = this.inner.plugins[CorsPlugin::class.java] != null

}
