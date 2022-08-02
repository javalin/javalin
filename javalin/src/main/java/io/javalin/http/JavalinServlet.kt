/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType.AFTER
import io.javalin.http.HandlerType.BEFORE
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.util.MethodNotAllowedUtil
import io.javalin.plugin.CorsPlugin
import io.javalin.routing.HandlerEntry
import io.javalin.routing.PathMatcher
import io.javalin.util.LogUtil
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.util.*

class JavalinServlet(val cfg: JavalinConfig) : HttpServlet() {

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
                submitTask { entry.handler.handle(ctx.update(entry, requestUri)) }
            }
        },
        Stage(DefaultName.HTTP) { submitTask ->
            matcher.findEntries(ctx.method(), requestUri).firstOrNull()?.let { entry ->
                submitTask { entry.handler.handle(ctx.update(entry, requestUri)) }
                return@Stage // return after first match
            }
            submitTask {
                if (ctx.method() == HEAD && matcher.hasEntries(GET, requestUri)) { // return 200, there is a get handler
                    return@submitTask
                }
                if (ctx.method() == HEAD || ctx.method() == GET) { // check for static resources (will write response if found)
                    if (cfg.pvt.resourceHandler?.handle(it.ctx.req(), JavalinResourceResponseWrapper(it.ctx)) == true) return@submitTask
                    if (cfg.pvt.singlePageHandler.handle(ctx)) return@submitTask
                }
                if (ctx.method() == OPTIONS && cfg.isCorsEnabled()) { // CORS is enabled, so we return 200 for OPTIONS
                    return@submitTask
                }
                if (ctx.handlerType() == BEFORE) { // no match, status will be 404 or 405 after this point
                    ctx.endpointHandlerPath = "No handler matched request path/method (404/405)"
                }
                val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(matcher, requestUri)
                if (cfg.http.prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                    throw MethodNotAllowedResponse(details = MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
                }
                throw NotFoundResponse()
            }
        },
        Stage(DefaultName.ERROR, haltsOnError = false) { submitTask ->
            submitTask { errorMapper.handle(ctx.statusCode(), ctx) }
        },
        Stage(DefaultName.AFTER, haltsOnError = false) { submitTask ->
            matcher.findEntries(AFTER, requestUri).forEach { entry ->
                submitTask { entry.handler.handle(ctx.update(entry, requestUri)) }
            }
        }
    )

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val ctx = DefaultContext(
                req = request,
                res = response,
                appAttributes = cfg.pvt.appAttributes,
                compressionStrategy = cfg.pvt.compressionStrategy
            )

            LogUtil.setup(ctx, matcher, cfg.pvt.requestLogger != null)
            ctx.contentType(cfg.http.defaultContentType)

            JavalinServletHandler(
                stages = ArrayDeque(lifecycle),
                cfg = cfg,
                errorMapper = errorMapper,
                exceptionMapper = exceptionMapper,
                ctx = ctx
            ).queueNextTaskOrFinish()
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable)
        }
    }

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<io.javalin.security.RouteRole>) {
        val protectedHandler = if (handlerType.isHttpMethod()) Handler { ctx -> cfg.pvt.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, cfg.routing, protectedHandler, handler))
    }

    private fun JavalinConfig.isCorsEnabled() = this.pvt.plugins[CorsPlugin::class.java] != null

    private class JavalinResourceResponseWrapper(private val ctx: Context) : HttpServletResponseWrapper(ctx.res()) {
        override fun getOutputStream() = ctx.outputStream()
    }

}
