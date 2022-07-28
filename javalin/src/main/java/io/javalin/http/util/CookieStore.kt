/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Cookie
import io.javalin.http.getCookie
import io.javalin.http.removeCookie
import io.javalin.http.setJavalinCookie
import io.javalin.plugin.json.JsonMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.*

@Suppress("UNCHECKED_CAST")
class CookieStore(var req: HttpServletRequest, var res: HttpServletResponse, val jsonMapper: JsonMapper) {

    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private val cookieMap = deserialize(req.getCookie(COOKIE_NAME))

    /**
     * Gets cookie store value for specified key.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    operator fun <T> get(key: String) = cookieMap[key] as T

    /**
     * Sets cookie store value for specified key.
     * Values are made available for other handlers, requests, and servers.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    operator fun set(key: String, value: Any) {
        cookieMap[key] = value
        res.setJavalinCookie(Cookie(COOKIE_NAME, serialize(cookieMap)))
    }

    /**
     * Clears cookie store in the context and from the response.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun clear() {
        cookieMap.clear()
        res.removeCookie(COOKIE_NAME, "/")
    }

    private fun deserialize(cookie: String?) = if (!cookie.isNullOrEmpty()) {
        jsonMapper.fromJsonString(String(decoder.decode(cookie)), Map::class.java) as MutableMap<String, Any>
    } else mutableMapOf()

    private fun serialize(map: MutableMap<String, Any>) =
        encoder.encodeToString(jsonMapper.toJsonString(map).toByteArray())

    companion object {
        var COOKIE_NAME = "javalin-cookie-store"
    }

}
