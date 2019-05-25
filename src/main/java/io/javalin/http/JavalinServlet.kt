/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import io.javalin.core.security.CoreRoles
import io.javalin.core.security.Role
import io.javalin.core.util.Header
import io.javalin.core.util.LogUtil
import io.javalin.core.util.Util
import io.javalin.http.util.ContextUtil
import io.javalin.http.util.MethodNotAllowedUtil
import java.io.InputStream
import java.util.concurrent.CompletionException
import java.util.zip.GZIPOutputStream
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServlet(val config: JavalinConfig): HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    override fun service(servletRequest: HttpServletRequest, res: HttpServletResponse) = try {

        val req = CachedRequestWrapper(servletRequest, config.requestCacheSize) // cached for reading multiple times
        val type = HandlerType.fromServletRequest(req)
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
            if (type == HandlerType.HEAD || type == HandlerType.GET) { // let Jetty check for static resources
                if (config.inner.resourceHandler?.handle(req, res) == true) return@tryWithExceptionMapper
                if (config.inner.singlePageHandler.handle(ctx)) return@tryWithExceptionMapper
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

        fun writeResult(res: HttpServletResponse) { // can be sync or async
            if (res.isCommitted || ctx.resultStream() == null) return // nothing to write
            val resultStream = ctx.resultStream()!!
            if (res.getHeader(Header.ETAG) != null || (config.autogenerateEtags && type == HandlerType.GET)) {
                val serverEtag = res.getHeader(Header.ETAG) ?: Util.getChecksumAndReset(resultStream) // calculate if not set
                res.setHeader(Header.ETAG, serverEtag)
                if (serverEtag == req.getHeader(Header.IF_NONE_MATCH)) {
                    res.status = 304
                    return // don't write body
                }
            }
            if (gzipShouldBeDone(ctx)) {
                GZIPOutputStream(res.outputStream, true).use { gzippedStream ->
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    resultStream.copyTo(gzippedStream)
                }
                resultStream.close()
                return
            }
            resultStream.copyTo(res.outputStream) // no gzip
            resultStream.close()
        }

        LogUtil.setup(ctx, matcher) // start request lifecycle
        ctx.header(Header.SERVER, "Javalin")
        ctx.contentType(config.defaultContentType)
        tryBeforeAndEndpointHandlers()
        if (ctx.resultFuture() == null) { // finish request synchronously
            tryErrorHandlers()
            tryAfterHandlers()
            writeResult(res)
            config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
        } else { // finish request asynchronously
            val asyncContext = req.startAsync().apply { timeout = config.asyncRequestTimeout }
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
                writeResult(asyncContext.response as HttpServletResponse)
                config.inner.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
                asyncContext.complete() // async lifecycle complete
            }
        }
        Unit // return void
    } catch (t: Throwable) {
        res.status = 500
        Javalin.log.error("Exception occurred while servicing http-request", t)
    }

    private fun hasGetHandlerMapped(requestUri: String) = matcher.findEntries(HandlerType.GET, requestUri).isNotEmpty()

    private fun gzipShouldBeDone(ctx: Context) = config.dynamicGzip
            && ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
            && (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains("gzip", ignoreCase = true)

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<Role>) {
        val shouldWrap = handlerType.isHttpMethod() && !roles.contains(CoreRoles.NO_WRAP)
        val protectedHandler = if (shouldWrap) Handler { ctx -> config.inner.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, protectedHandler, handler))
    }

}
