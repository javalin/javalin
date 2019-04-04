/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.*
import io.javalin.config.SamConversionsServlet
import io.javalin.core.util.*
import io.javalin.security.AccessManager
import io.javalin.security.CoreRoles
import io.javalin.security.Role
import io.javalin.security.SecurityUtil
import io.javalin.staticfiles.JettyResourceHandler
import io.javalin.staticfiles.Location
import io.javalin.staticfiles.ResourceHandler
import io.javalin.staticfiles.StaticFileConfig
import java.io.InputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServletConfig(
        val servlet: JavalinServlet,
        var dynamicGzip: Boolean = true,
        var autogenerateEtags: Boolean = false,
        var prefer405over404: Boolean = false,
        var defaultContentType: String = "text/plain",
        var requestCacheSize: Long = 4096L,
        var requestLogger: RequestLogger? = null,
        val singlePageHandler: SinglePageHandler = SinglePageHandler(),
        var resourceHandler: ResourceHandler? = null,
        var accessManager: AccessManager = AccessManager { handler, ctx, permittedRoles -> SecurityUtil.noopAccessManager(handler, ctx, permittedRoles) }
) : SamConversionsServlet {

    fun enableWebjars() = addStaticFiles("/webjars", Location.CLASSPATH)
    fun addStaticFiles(classpathPath: String) = addStaticFiles(classpathPath, Location.CLASSPATH)
    fun addStaticFiles(path: String, location: Location) {
        resourceHandler = resourceHandler ?: JettyResourceHandler()
        resourceHandler?.addStaticFileConfig(StaticFileConfig(path, location))
    }

    fun addSinglePageRoot(path: String, filePath: String) = addSinglePageRoot(path, filePath, Location.CLASSPATH)
    fun addSinglePageRoot(path: String, filePath: String, location: Location) = singlePageHandler.add(path, filePath, location)

    fun enableCorsForAllOrigins() = enableCorsForOrigins("*")
    fun enableCorsForOrigins(vararg origin: String) {
        CorsUtil.enableCorsForOrigin(servlet, arrayOf(*origin))
    }

    override fun accessManager(accessManager: AccessManager) {
        this.accessManager = accessManager
    }

    override fun requestLogger(requestLogger: RequestLogger) {
        this.requestLogger = requestLogger
    }
}

class JavalinServlet(private val appAttributes: Map<Class<*>, Any>) {

    val config = JavalinServletConfig(this)

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper()
    val errorMapper = ErrorMapper()

    fun service(servletRequest: HttpServletRequest, res: HttpServletResponse) {

        val req = CachedRequestWrapper(servletRequest, config.requestCacheSize) // cached for reading multiple times
        val type = HandlerType.fromServletRequest(req)
        val requestUri = req.requestURI.removePrefix(req.contextPath)
        val ctx = Context(req, res, appAttributes)

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
                if (config.resourceHandler?.handle(req, res) == true) return@tryWithExceptionMapper
                if (config.singlePageHandler.handle(ctx)) return@tryWithExceptionMapper
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
            config.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
            return // sync lifecycle complete
        } else { // finish request asynchronously
            val asyncContext = req.startAsync()
            ctx.resultFuture()!!.exceptionally { throwable ->
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
                writeResult(asyncContext.response as HttpServletResponse)
                config.requestLogger?.handle(ctx, LogUtil.executionTimeMs(ctx))
                asyncContext.complete() // async lifecycle complete
            }
        }
    }

    private fun hasGetHandlerMapped(requestUri: String) = matcher.findEntries(HandlerType.GET, requestUri).isNotEmpty()

    private fun gzipShouldBeDone(ctx: Context) = config.dynamicGzip
            && ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
            && (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains("gzip", ignoreCase = true)

    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, roles: Set<Role>) {
        val shouldWrap = handlerType.isHttpMethod() && !roles.contains(CoreRoles.NO_WRAP)
        val protectedHandler = if (shouldWrap) Handler { ctx -> config.accessManager.manage(handler, ctx, roles) } else handler
        matcher.add(HandlerEntry(handlerType, path, protectedHandler, handler))
    }
}
