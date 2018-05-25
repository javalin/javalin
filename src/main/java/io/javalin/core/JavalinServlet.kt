/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.HaltException
import io.javalin.LogLevel
import io.javalin.core.util.ContextUtil
import io.javalin.core.util.Header
import io.javalin.core.util.LogUtil
import io.javalin.embeddedserver.CachedRequestWrapper
import io.javalin.embeddedserver.CachedResponseWrapper
import io.javalin.embeddedserver.StaticResourceHandler
import io.javalin.embeddedserver.jetty.websocket.WebSocketHandler
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.properties.Delegates

class JavalinServlet(
        val contextPath: String,
        val matcher: PathMatcher,
        val exceptionMapper: ExceptionMapper,
        val errorMapper: ErrorMapper,
        val jettyWsHandlers: Map<String, Any>,
        val javalinWsHandlers: List<WebSocketHandler>,
        val logLevel: LogLevel,
        val dynamicGzipEnabled: Boolean,
        val defaultContentType: String,
        val defaultCharacterEncoding: String,
        val maxRequestCacheBodySize: Long) {

    private val log = LoggerFactory.getLogger(JavalinServlet::class.java)

    var staticResourceHandler: StaticResourceHandler by Delegates.notNull()

    fun service(servletRequest: ServletRequest, servletResponse: ServletResponse) {

        val req = CachedRequestWrapper(servletRequest as HttpServletRequest, maxRequestCacheBodySize) // cached for reading multiple times
        val res =
                if (logLevel == LogLevel.EXTENSIVE) CachedResponseWrapper(servletResponse as HttpServletResponse) // body needs to be copied for logging
                else servletResponse as HttpServletResponse
        val type = HandlerType.fromServletRequest(req)
        val requestUri = req.requestURI
        val ctx = ContextUtil.create(res, req)

        ctx.header("Server", "Javalin")
        ctx.attribute("javalin-request-log-start-time", System.nanoTime())

        res.characterEncoding = defaultCharacterEncoding
        res.contentType = defaultContentType

        fun tryWithExceptionMapper(func: () -> Unit) = exceptionMapper.catchException(ctx, func)

        fun tryBeforeAndEndpointHandlers() = tryWithExceptionMapper {
            matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
            }
            val endpointEntries = matcher.findEntries(type, requestUri)
            endpointEntries.forEach { entry ->
                ctx.futureCanBeSet = true
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                ctx.futureCanBeSet = false
                if (!ctx.nexted()) {
                    return@tryWithExceptionMapper
                }
            }
            if (endpointEntries.isEmpty() && type != HandlerType.HEAD && type != HandlerType.GET) {
                throw HaltException(404, "Not found")
            }
            if (shouldCheckForStaticFiles(endpointEntries, type, requestUri)) {
                staticResourceHandler.handle(req, res)
            }
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
                    asyncContext.complete()
                }
            }
        }
    }

    private fun writeResult(ctx: Context, res: HttpServletResponse) {
        if (!res.isCommitted) {
            ctx.resultStream()?.let { resultStream ->
                if (gzipShouldBeDone(ctx)) {
                    GZIPOutputStream(res.outputStream, true).use { gzippedStream ->
                        res.setHeader(Header.CONTENT_ENCODING, "gzip")
                        resultStream.copyTo(gzippedStream)
                    }
                } else {
                    resultStream.copyTo(res.outputStream)
                }
            }
        }
        LogUtil.logRequestAndResponse(ctx, logLevel, matcher, log, gzipShouldBeDone(ctx))
    }

    private fun shouldCheckForStaticFiles(endpointEntries: List<HandlerEntry>, type: HandlerType, requestUri: String) = when {
        type == HandlerType.GET && endpointEntries.isEmpty() -> true
        type == HandlerType.HEAD && matcher.findEntries(HandlerType.GET, requestUri).isEmpty() -> true
        else -> false
    }

    private fun gzipShouldBeDone(ctx: Context) = dynamicGzipEnabled
            && ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
            && (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains("gzip", ignoreCase = true)
}
