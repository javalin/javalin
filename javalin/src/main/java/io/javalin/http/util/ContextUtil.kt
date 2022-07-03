/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.core.security.BasicAuthCredentials
import io.javalin.core.util.Header
import io.javalin.core.util.JavalinLogger
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HandlerEntry
import io.javalin.http.HandlerType
import io.javalin.http.HttpCode
import io.javalin.http.HttpResponseException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.InputStream
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*

object ContextUtil {

    fun update(ctx: Context, handlerEntry: HandlerEntry, requestUri: String) = ctx.apply {
        matchedPath = handlerEntry.path
        pathParamMap = handlerEntry.extractPathParams(requestUri)
        handlerType = handlerEntry.type
        if (handlerType != HandlerType.AFTER) {
            endpointHandlerPath = handlerEntry.path
        }
    }

    // this header is semi-colon separated, like: "text/html; charset=UTF-8"
    fun getRequestCharset(ctx: Context) = ctx.req.getHeader(Header.CONTENT_TYPE)?.let { value ->
        value.split(";").find { it.trim().startsWith("charset", ignoreCase = true) }?.let { it.split("=")[1].trim() }
    }

    fun splitKeyValueStringAndGroupByKey(string: String, charset: String): Map<String, List<String>> {
        return if (string.isEmpty()) mapOf() else string.split("&").map { it.split("=", limit = 2) }.groupBy(
            { URLDecoder.decode(it[0], charset) },
            { if (it.size > 1) URLDecoder.decode(it[1], charset) else "" }
        ).mapValues { it.value.toList() }
    }

    fun pathParamOrThrow(pathParams: Map<String, String?>, key: String, url: String) =
        pathParams[key.replaceFirst("{", "").replaceFirst("}", "")] ?: throw IllegalArgumentException("'$key' is not a valid path-param for '$url'.")

    fun urlDecode(s: String): String = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")

    fun hasBasicAuthCredentials(authorizationHeader: String?) =
        runCatching { getBasicAuthCredentials(authorizationHeader) }.isSuccess

    /**
     * @throws IllegalStateException if specified string is not valid Basic auth header
     */
    fun getBasicAuthCredentials(authorizationHeader: String?): BasicAuthCredentials =
        authorizationHeader
            ?.takeIf { authorizationHeader.startsWith("Basic ") }
            ?.removePrefix("Basic ")
            ?.let { Base64.getDecoder().decode(it) }
            ?.decodeToString()
            ?.split(':', limit = 2)
            ?.let { (username, password) -> BasicAuthCredentials(username, password) }
            ?: throw IllegalArgumentException("Invalid Basic auth header. Value was '$authorizationHeader'.")

    fun acceptsHtml(ctx: Context) =
        ctx.header(Header.ACCEPT)?.contains(ContentType.HTML) == true

    @JvmStatic
    @JvmOverloads
    fun init(
        request: HttpServletRequest,
        response: HttpServletResponse,
        matchedPath: String = "*",
        pathParamMap: Map<String, String> = mapOf(),
        handlerType: HandlerType = HandlerType.INVALID,
        appAttributes: Map<String, Any> = mapOf()
    ) = Context(request, response, appAttributes).apply {
        this.matchedPath = matchedPath
        this.pathParamMap = pathParamMap
        this.handlerType = handlerType
    }

    fun Context.isLocalhost() = try {
        URL(this.url()).host.let { it == "localhost" || it == "127.0.0.1" }
    } catch (e: Exception) {
        false
    }

    fun changeBaseRequest(ctx: Context, req: HttpServletRequest) = Context(req, ctx.res, ctx.appAttributes).apply {
        this.pathParamMap = ctx.pathParamMap
        this.matchedPath = ctx.matchedPath
    }

    fun Context.throwPayloadTooLargeIfPayloadTooLarge() {
        val maxRequestSize = this.appAttribute<Long>(MAX_REQUEST_SIZE_KEY)
        if (this.req.contentLength > maxRequestSize) {
            JavalinLogger.warn("Body greater than max size ($maxRequestSize bytes)")
            throw HttpResponseException(HttpCode.PAYLOAD_TOO_LARGE.status, HttpCode.PAYLOAD_TOO_LARGE.message)
        }
    }

    const val MAX_REQUEST_SIZE_KEY = "javalin-max-request-size"

    const val sessionCacheKeyPrefix = "javalin-session-attribute-cache-"

    fun cacheAndSetSessionAttribute(key: String, value: Any?, req: HttpServletRequest) {
        req.setAttribute("$sessionCacheKeyPrefix$key", value)
        req.session.setAttribute(key, value)
    }

    fun <T> getCachedRequestAttributeOrSessionAttribute(key: String, req: HttpServletRequest): T? {
        val cachedAttribute = req.getAttribute("$sessionCacheKeyPrefix$key")
        if (cachedAttribute == null) {
            req.setAttribute("$sessionCacheKeyPrefix$key", req.session.getAttribute(key))
        }
        return req.getAttribute("$sessionCacheKeyPrefix$key") as T?
    }

    fun <T> cachedSessionAttributeOrCompute(callback: (Context) -> T, key: String, ctx: Context): T? {
        if (getCachedRequestAttributeOrSessionAttribute<T?>(key, ctx.req) == null) {
            cacheAndSetSessionAttribute(key, callback(ctx), ctx.req) // set new value from callback
        }
        return getCachedRequestAttributeOrSessionAttribute(key, ctx.req) // existing or computed (or null)
    }

    fun readAndResetStreamIfPossible(stream: InputStream?, charset: Charset) = try {
        stream?.apply { reset() }?.readBytes()?.toString(charset).also { stream?.reset() }
    } catch (e: Exception) {
        "resultString unavailable (resultStream couldn't be reset)"
    }

}
