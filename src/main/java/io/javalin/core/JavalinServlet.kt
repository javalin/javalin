/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.HaltException
import io.javalin.RequestLogger
import io.javalin.core.util.*
import io.javalin.staticfiles.JettyResourceHandler
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(
        val matcher: PathMatcher,
        val exceptionMapper: ExceptionMapper,
        val errorMapper: ErrorMapper,
        val debugLogging: Boolean,
        val requestLogger: RequestLogger?,
        val dynamicGzipEnabled: Boolean,
        val dynamicEtagsEnabled: Boolean,
        val defaultContentType: String,
        val defaultCharacterEncoding: String,
        val maxRequestCacheBodySize: Long,
        val prefer405over404: Boolean,
        val jettyResourceHandler: JettyResourceHandler) {

    private val log = LoggerFactory.getLogger(JavalinServlet::class.java)

    fun service(servletRequest: ServletRequest, servletResponse: ServletResponse) {

        val req = CachedRequestWrapper(servletRequest as HttpServletRequest, maxRequestCacheBodySize) // cached for reading multiple times
        val res =
                if (debugLogging) CachedResponseWrapper(servletResponse as HttpServletResponse) // body needs to be copied for logging
                else servletResponse as HttpServletResponse
        val type = HandlerType.fromServletRequest(req)
        val requestUri = req.requestURI.toLowerCase()
        val ctx = Context(res, req)

        ctx.header(Header.SERVER, "Javalin")
        ctx.attribute("javalin-request-log-start-time", System.nanoTime())

        res.characterEncoding = defaultCharacterEncoding
        res.contentType = defaultContentType

        fun tryWithExceptionMapper(func: () -> Unit) = exceptionMapper.catchException(ctx, func)

        fun tryBeforeAndEndpointHandlers() = tryWithExceptionMapper {
            matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
            }
            matcher.findEntries(type, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                return@tryWithExceptionMapper // return after first match
            }
            if (type == HandlerType.HEAD && hasGetHandlerMapped(requestUri)) {
                return@tryWithExceptionMapper // return 200, there is a get handler
            }
            if (type == HandlerType.HEAD || type == HandlerType.GET) { // let Jetty check for static resources
                jettyResourceHandler.handle(req, res)
            }
            val availableHandlerTypes = MethodNotAllowedUtil.findAvailableHttpHandlerTypes(matcher, requestUri)
            if (prefer405over404 && availableHandlerTypes.isNotEmpty()) {
                throw HaltException(405, MethodNotAllowedUtil.getAvailableHandlerTypes(ctx, availableHandlerTypes))
            }
            throw HaltException(404, "Not found")
        }

        fun tryErrorHandlers() = tryWithExceptionMapper {
            errorMapper.handle(ctx.status(), ctx)
        }

        fun tryAfterHandlers() = tryWithExceptionMapper {
            matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
            }
        }

        tryBeforeAndEndpointHandlers() // start request lifecycle

        val future = ctx.resultFuture()
        if (future == null) {
            tryErrorHandlers()
            tryAfterHandlers()
            writeResult(ctx, res)
            logRequest(ctx)
        } else {
            req.startAsync().let { asyncContext ->
                future.exceptionally { throwable ->
                    if (throwable is Exception) {
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
                    writeResult(ctx, asyncContext.response as HttpServletResponse)
                    logRequest(ctx)
                    asyncContext.complete()
                }
            }
        }
    }

    private fun writeResult(ctx: Context, res: HttpServletResponse) {
        if (res.isCommitted || ctx.resultStream() == null) return // nothing to write
        val resultStream = ctx.resultStream()!!
        if (dynamicEtagsEnabled) {
            val serverEtag = Util.getChecksumAndReset(resultStream)
            ctx.header(Header.ETAG, serverEtag)
            if (serverEtag == ctx.header(Header.IF_NONE_MATCH)) {
                ctx.status(304)
                return // don't write body
            }
        }
        if (gzipShouldBeDone(ctx)) {
            return GZIPOutputStream(res.outputStream, true).use { gzippedStream ->
                ctx.header(Header.CONTENT_ENCODING, "gzip")
                resultStream.copyTo(gzippedStream)
            }
        } else {
            resultStream.copyTo(res.outputStream)
        }
    }

    private fun logRequest(ctx: Context) {
        if (requestLogger != null) {
            requestLogger.handle(ctx, LogUtil.executionTimeMs(ctx))
        } else if (debugLogging == true) {
            LogUtil.logRequestAndResponse(ctx, matcher, log, gzipShouldBeDone(ctx))
        }
    }

    private fun hasGetHandlerMapped(requestUri: String) = matcher.findEntries(HandlerType.GET, requestUri).isNotEmpty()

    private fun gzipShouldBeDone(ctx: Context) = dynamicGzipEnabled
            && ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
            && (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains("gzip", ignoreCase = true)
}
