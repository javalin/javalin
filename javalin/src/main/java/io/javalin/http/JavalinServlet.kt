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
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MethodNotAllowedUtil
import javax.servlet.AsyncContext
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        try {
            val type = HandlerType.fromServletRequest(req)
            val rwc = ResponseWrapperContext(req, config)
            val requestUri = req.requestURI.removePrefix(req.contextPath)
            val ctx = Context(req, res, config.inner.appAttributes)

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
                if (type == HandlerType.HEAD || type == HandlerType.GET) { // check for static resources (will write response if found)
                    if (config.inner.resourceHandler?.handle(req, JavalinResponseWrapper(res, rwc)) == true) return@tryWithExceptionMapper
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

            var responseStarted = false
            fun finishUpResponse(response: ServletResponse) {
                if (responseStarted) return // prevent writing twice (ex. both async requests+errors)
                responseStarted = true
                tryWithExceptionMapper { // run error mappers (can mutate ctx)
                    errorMapper.handle(ctx.status(), ctx)
                }
                tryWithExceptionMapper { // run after handlers (can mutate ctx)
                    matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
                        entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                    }
                }

                // write the response
                JavalinResponseWrapper(response as HttpServletResponse, rwc).write(ctx.resultStream())
                config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx)) // log stuff
            }

            LogUtil.setup(ctx, matcher, config.inner.requestLogger != null) // start request lifecycle
            ctx.contentType(config.defaultContentType)
            tryBeforeAndEndpointHandlers()
            val resultFuture = ctx.resultFuture() // store the future, so it's not overwritten by `result` below
            if (resultFuture == null) {
                return finishUpResponse(res) // request lifecycle is complete (blocking/synchronous)
            }
            // user has set a Future result, we call startAsync and setup callbacks
            val asyncContext = req.startAsync().apply { timeout = config.asyncRequestTimeout }
            asyncContext.addTimeoutListener {
                ctx.status(500).result("Request timed out")
                finishUpResponse(asyncContext.response).also { asyncContext.complete() }
                resultFuture.cancel(true)
            }
            resultFuture.exceptionally { throwable ->
                exceptionMapper.handleFutureException(ctx, throwable)
            }.thenAccept { futureValue ->
                ctx.futureConsumer?.accept(futureValue) // this consumer can set result, status, etc
                finishUpResponse(asyncContext.response).also { asyncContext.complete() }
            }.exceptionally { throwable -> // exception might occur when writing response
                exceptionMapper.handleUnexpectedThrowable(res, throwable).also { asyncContext.complete() }
            }
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(res, throwable)
        }
    }

    private fun hasGetHandlerMapped(requestUri: String) = matcher.findEntries(HandlerType.GET, requestUri).isNotEmpty()

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<RouteRole>) {
        val protectedHandler = if (handlerType.isHttpMethod()) Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, config.ignoreTrailingSlashes, protectedHandler, handler))
    }

    private fun isCorsEnabled(config: JavalinConfig) = config.inner.plugins[CorsPlugin::class.java] != null
}

private fun AsyncContext.addTimeoutListener(callback: () -> Unit) = this.addListener(object : AsyncListener {
    override fun onComplete(event: AsyncEvent) {}
    override fun onError(event: AsyncEvent) {}
    override fun onStartAsync(event: AsyncEvent) {}
    override fun onTimeout(event: AsyncEvent) = callback() // this is all we care about
})
