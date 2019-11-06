/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.core.security.BasicAuthCredentials
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.HandlerEntry
import io.javalin.http.HandlerType
import java.net.URLDecoder
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object ContextUtil {

    fun update(ctx: Context, handlerEntry: HandlerEntry, requestUri: String) = ctx.apply {
        matchedPath = handlerEntry.path
        pathParamMap = handlerEntry.extractPathParams(requestUri)
        handlerType = handlerEntry.type
        if (handlerType != HandlerType.AFTER) {
            endpointHandlerPath = handlerEntry.path
        }
    }

    fun splitKeyValueStringAndGroupByKey(string: String): Map<String, List<String>> {
        return if (string.isEmpty()) mapOf() else string.split("&").map { it.split("=", limit = 2) }.groupBy(
                { it[0] },
                { if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        ).mapValues { it.value.toList() }
    }

    fun pathParamOrThrow(pathParams: Map<String, String?>, key: String, url: String) =
            pathParams[key.replaceFirst(":", "")] ?: throw IllegalArgumentException("'$key' is not a valid path-param for '$url'.")

    fun urlDecode(s: String): String = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")

    fun hasBasicAuthCredentials(header: String?): Boolean {
        return try { getBasicAuthCredentials(header); true } catch (e: Exception) { false }
    }

    fun getBasicAuthCredentials(header: String?): BasicAuthCredentials {
        require(header?.startsWith("Basic ") == true) { "Invalid basicauth header. Value was '$header'." }
        val (username, password) = String(Base64.getDecoder().decode(header!!.removePrefix("Basic "))).split(':', limit = 2)
        return BasicAuthCredentials(username, password)
    }

    fun acceptsHtml(ctx: Context) = ctx.header(Header.ACCEPT)?.contains("text/html") == true

    @JvmStatic
    @JvmOverloads
    fun init(
            request: HttpServletRequest,
            response: HttpServletResponse,
            matchedPath: String = "*",
            pathParamMap: Map<String, String> = mapOf(),
            handlerType: HandlerType = HandlerType.INVALID,
            appAttributes: Map<Class<*>, Any> = mapOf()
    ) = Context(request, response, appAttributes).apply {
        this.matchedPath = matchedPath
        this.pathParamMap = pathParamMap
        this.handlerType = handlerType
    }

    fun Context.isLocalhost() = this.host()?.contains("localhost") == true || this.host()?.contains("127.0.0.1") == true

    fun changeBaseRequest(ctx: Context, req: HttpServletRequest) = Context(req, ctx.res).apply {
        this.pathParamMap = ctx.pathParamMap
        this.matchedPath = ctx.matchedPath
    }

}
