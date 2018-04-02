/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
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
import java.util.concurrent.CompletionStage
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

        fun tryWithExceptionMapper(f: () -> Unit) = exceptionMapper.catchException(ctx, f)

        tryWithExceptionMapper {
            matcher.findEntries(HandlerType.BEFORE, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
            }
            val endpointEntries = matcher.findEntries(type, requestUri)
            endpointEntries.forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
                if (!ctx.nexted()) {
                    return@tryWithExceptionMapper
                }
            }
            if (shouldCheckForStaticFiles(endpointEntries, type, requestUri)) {
                staticResourceHandler.handle(req, res)
            }
        }

        tryWithExceptionMapper {
            matcher.findEntries(HandlerType.AFTER, requestUri).forEach { entry ->
                entry.handler.handle(ContextUtil.update(ctx, entry, requestUri))
            }
        }

        tryWithExceptionMapper {
            errorMapper.handle(ctx.status(), ctx)
        }

        val future = ctx.resultFuture()
        if (future != null) {
            val async = req.startAsync()
            future.exceptionally {
                if (it is Exception) {
                    exceptionMapper.handle(it, ctx)
                }
                null // JInterop
            }
            .thenApply {
                tryWithExceptionMapper {
                    errorMapper.handle(ctx.status(), ctx)
                }
            }
            .thenAccept {
                writeResult(ctx, async.request as HttpServletRequest, async.response as HttpServletResponse)
            }
            .whenComplete { _, _ ->
                async.complete()
            }
        } else {
            writeResult(ctx, req, res)
        }

    }

    private fun writeResult(ctx: Context, req: HttpServletRequest, res: HttpServletResponse) {
        // write result to servlet-response (if not already committed)
        val doGzip = gzipShouldBeDone(ctx.resultStream(), req)
        if (!res.isCommitted) {
            ctx.resultStream()?.let { resultStream ->
                if (doGzip) {
                    GZIPOutputStream(res.outputStream, true).use { gzippedStream ->
                        res.setHeader(Header.CONTENT_ENCODING, "gzip")
                        resultStream.copyTo(gzippedStream)
                    }
                } else {
                    resultStream.copyTo(res.outputStream)
                }
            }
        }
        LogUtil.logRequestAndResponse(ctx, logLevel, matcher, log, doGzip)
    }

    private fun shouldCheckForStaticFiles(endpointEntries: List<HandlerEntry>, type: HandlerType, requestUri: String) = when {
        type != HandlerType.HEAD && endpointEntries.isEmpty() -> true
        type == HandlerType.HEAD && matcher.findEntries(HandlerType.GET, requestUri).isEmpty() -> true
        else -> false
    }

    private fun gzipShouldBeDone(resultStream: InputStream?, req: HttpServletRequest) = dynamicGzipEnabled
            && resultStream?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
            && (req.getHeader(Header.ACCEPT_ENCODING) ?: "").contains("gzip", ignoreCase = true)
}
