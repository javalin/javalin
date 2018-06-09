/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.BasicAuthCredentials
import io.javalin.Context
import io.javalin.core.HandlerEntry
import io.javalin.core.HandlerType
import java.net.URLDecoder
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object ContextUtil {

    fun create(response: HttpServletResponse, request: HttpServletRequest): Context {
        return Context(response, request, false, "", HashMap(), ArrayList(), HandlerType.BEFORE)
    }

    fun update(ctx: Context, handlerEntry: HandlerEntry, requestUri: String): Context {
        ctx.matchedPath = handlerEntry.path
        ctx.pathParamMap = handlerEntry.extractParams(requestUri)
        ctx.splatList = handlerEntry.extractSplats(requestUri)
        ctx.handlerType = handlerEntry.type
        return ctx
    }

    fun splitKeyValueStringAndGroupByKey(string: String): Map<String, List<String>> {
        return string.split("&").map { it.split("=") }.groupBy(
                { it[0] },
                { if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        ).mapValues { it.value.toList() }
    }

    fun urlDecode(s: String): String = URLDecoder.decode(s.replace("+", "%2B"), "UTF-8").replace("%2B", "+")

    fun mapKeysOrReturnNullIfAnyNulls(keys: Array<out String>, f: (s: String) -> String?): List<String>? = try {
        keys.map { f.invoke(it) }.requireNoNulls().toList()
    } catch (e: IllegalArgumentException) {
        null
    }

    fun getBasicAuthCredentials(header: String?): BasicAuthCredentials? = try {
        val (username, password) = String(Base64.getDecoder().decode(header!!.removePrefix("Basic "))).split(":")
        BasicAuthCredentials(username, password)
    } catch (e: Exception) {
        null
    }

}
