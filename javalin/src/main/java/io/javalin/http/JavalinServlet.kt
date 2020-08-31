/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.security.Role
import io.javalin.core.util.CorsPlugin
import io.javalin.core.util.Header
import io.javalin.core.util.LogUtil
import io.javalin.core.util.Util
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MethodNotAllowedUtil
import java.io.InputStream
import java.util.concurrent.CompletionException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    override fun service(rawReq: HttpServletRequest, rawRes: HttpServletResponse) {
        try {
            val wrappedReq = CachedRequestWrapper(rawReq, config.requestCacheSize) // cached for reading multiple times
            val type = HandlerType.fromServletRequest(wrappedReq)
            val rwc = ResponseWrapperContext(rawReq, config)
            val requestUri = wrappedReq.requestURI.removePrefix(wrappedReq.contextPath)
            val ctx = Context(wrappedReq, rawRes, config.inner.appAttributes)

            fun tryWithExceptionMapper(func: () -> Unit) = exceptionMapper.catchException(ctx, func)

            fun tryBeforeAndEndpointHandlers() = tryWithExceptionMapper {
                matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
                    entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                }
                matcher.findEntries(type, requestUri).firstOrNull()?.let { entry ->
                    entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                    return@tryWithExceptionMapper // return after first match
                }
                if (type == HandlerType.HEAD && hasGetHandlerMapped(requestUri)) {
                    return@tryWithExceptionMapper // return 200, there is a get handler
                }
                if (type == HandlerType.HEAD || type == HandlerType.GET) { // let Jetty check for static resources (will write response if found)
                    if (config.inner.resourceHandler?.handle(wrappedReq, JavalinResponseWrapper(rawRes, rwc)) == true) return@tryWithExceptionMapper
                    if (config.inner.singlePageHandler.handle(ctx)) return@tryWithExceptionMapper
                }
                if (type == HandlerType.OPTIONS && isCorsEnabled(config)) { // CORS is enabled, so we return 200 for OPTIONS
                    return@tryWithExceptionMapper
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

            fun tryErrorHandlers() = tryWithExceptionMapper {
                errorMapper.handle(ctx.status(), ctx)
            }

            fun tryAfterHandlers() = tryWithExceptionMapper {
                matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
                    entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                }
            }

            LogUtil.setup(ctx, matcher) // start request lifecycle
            ctx.header(Header.SERVER, "Javalin")
            ctx.contentType(config.defaultContentType)
            tryBeforeAndEndpointHandlers()
            if (ctx.resultFuture() == null) { // finish request synchronously
                tryErrorHandlers()
                tryAfterHandlers()
                JavalinResponseWrapper(rawRes, rwc).write(ctx.resultStream())
                config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
            } else { // finish request asynchronously
                val asyncContext = wrappedReq.startAsync().apply { timeout = config.asyncRequestTimeout }
                ctx.resultFuture()!!.exceptionally { throwable ->
                    if (throwable is CompletionException && throwable.cause is Exception) {
                        exceptionMapper.handle(throwable.cause as Exception, ctx)
                    } else if (throwable is Exception) {
                        exceptionMapper.handle(throwable, ctx)
                    }
                    null
                }.thenAccept {
                    when (it) {
                        is InputStream -> ctx.result(it)
                        is String -> ctx.result(it)
                    }
                    tryErrorHandlers()
                    tryAfterHandlers()
                    val asyncRes = asyncContext.response as HttpServletResponse
                    JavalinResponseWrapper(asyncRes, rwc).write(ctx.resultStream())
                    config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
                    asyncContext.complete() // async lifecycle complete
                }
            }
            Unit // return void
        } catch (t: Throwable) {
            if (!Util.isClientAbortException(t)) {
                rawRes.status = 500
                Javalin.log?.error("Exception occurred while servicing http-request", t)
            }
        }
    }

    private fun hasGetHandlerMapped(requestUri: String) = matcher.findEntries(HandlerType.GET, requestUri).isNotEmpty()

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<Role>) {
        val protectedHandler = if (handlerType.isHttpMethod()) Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, config.ignoreTrailingSlashes, protectedHandler, handler))
    }

    private fun isCorsEnabled(config: JavalinConfig) = config.inner.plugins[CorsPlugin::class.java] != null
}
