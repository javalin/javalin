/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.HaltException
import io.javalin.LogLevel
import io.javalin.core.util.ContextUtil
import io.javalin.core.util.LogUtil
import io.javalin.embeddedserver.CachedRequestWrapper
import io.javalin.embeddedserver.CachedResponseWrapper
import io.javalin.embeddedserver.StaticResourceHandler
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(
        val contextPath: String,
        val matcher: PathMatcher,
        val exceptionMapper: ExceptionMapper,
        val errorMapper: ErrorMapper,
        val wsHandlers: Map<String, Any>,
        val logLevel: LogLevel) {

    private val log = LoggerFactory.getLogger(JavalinServlet::class.java)

    var staticResourceHandler: StaticResourceHandler? = null

    fun service(servletRequest: ServletRequest, servletResponse: ServletResponse) {

        val req = CachedRequestWrapper(servletRequest as HttpServletRequest) // cached for reading multiple times
        val res =
                if (logLevel == LogLevel.EXTENSIVE) CachedResponseWrapper(servletResponse as HttpServletResponse) // body needs to be copied for logging
                else servletResponse as HttpServletResponse
        val type = HandlerType.fromServletRequest(req)
        val requestUri = req.requestURI

        val beforeEntries = matcher.findEntries(HandlerType.BEFORE, requestUri)
        val endpointEntries = matcher.findEntries(type, requestUri)
        val afterEntries = matcher.findEntries(HandlerType.AFTER, requestUri)

        val ctx = ContextUtil.create(res, req, (beforeEntries + endpointEntries + afterEntries)).apply {
            header("Server", "Javalin")
            attribute("javalin-request-log-start-time", System.nanoTime())
        }

        try { // before-handlers, endpoint-handlers, static-files

            for (beforeEntry in beforeEntries) {
                beforeEntry.handler.handle(ContextUtil.update(ctx, beforeEntry, requestUri))
            }

            if (!endpointEntries.isEmpty()) {
                for (endpointEntry in endpointEntries) {
                    endpointEntry.handler.handle(ContextUtil.update(ctx, endpointEntry, requestUri))
                    if (!ctx.nexted()) {
                        break
                    }
                }
            } else if (type !== HandlerType.HEAD || type === HandlerType.HEAD && matcher.findEntries(HandlerType.GET, requestUri).isEmpty()) {
                if (!staticResourceHandler!!.handle(req, res)) {
                    throw HaltException(404, "Not found")
                }
            }

        } catch (e: Exception) {
            // both before-handlers and endpoint-handlers can throw Exception,
            // we need to handle these exceptions here in order to run after-handlers even if an exception was thrown
            exceptionMapper.handle(e, ctx)
        }

        try { // after-handlers
            for (afterEntry in afterEntries) {
                afterEntry.handler.handle(ContextUtil.update(ctx, afterEntry, requestUri))
            }
        } catch (e: Exception) {
            // after-handlers can also throw exceptions
            exceptionMapper.handle(e, ctx)
        }

        try { // error mapping (turning status codes into standardized messages/pages)
            errorMapper.handle(ctx.status(), ctx)
        } catch (e: RuntimeException) {
            // depressingly, the error mapping itself could throw a runtime exception
            // we need to handle these last... but that's it.
            exceptionMapper.handle(e, ctx)
        }

        // write result to servlet-response (if not already committed)
        if (!res.isCommitted) {
            if (res.contentType == null) {
                res.contentType = "text/plain"
            }
            if (res.characterEncoding == "iso-8859-1") {
                res.characterEncoding = StandardCharsets.UTF_8.name()
            }
            ctx.resultString()?.let { resultString ->
                ctx.result(ByteArrayInputStream(resultString.toByteArray()))
            }
            ctx.resultStream()?.let { resultStream ->
                resultStream.copyTo(res.outputStream)
                resultStream.close()
                res.outputStream.close()
            }
        }

        LogUtil.logRequestAndResponse(ctx, logLevel, matcher, type, requestUri, log)

    }

}
