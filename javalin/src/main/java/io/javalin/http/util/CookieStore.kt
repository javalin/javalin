/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.Cookie
import io.javalin.json.fromJsonString
import io.javalin.json.jsonMapper
import io.javalin.json.toJsonString
import java.util.*

@Suppress("UNCHECKED_CAST")
class CookieStore(val ctx: Context) {

    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()
    private val cookieMap = deserialize(ctx.cookie(COOKIE_NAME))

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
        ctx.cookie(Cookie(COOKIE_NAME, serialize(cookieMap)))
    }

    /**
     * Clears cookie store in the context and from the response.
     * @see <a href="https://javalin.io/documentation#cookie-store">Cookie store in docs</a>
     */
    fun clear() {
        cookieMap.clear()
        ctx.removeCookie(COOKIE_NAME)
    }

    private fun deserialize(cookie: String?): MutableMap<String, Any> = when {
        !cookie.isNullOrEmpty() -> ctx.jsonMapper().fromJsonString(String(decoder.decode(cookie)))
        else -> mutableMapOf()
    }

    private fun serialize(map: MutableMap<String, Any>): String =
        encoder.encodeToString(ctx.jsonMapper().toJsonString(map).toByteArray())

    companion object {
        var COOKIE_NAME = "javalin-cookie-store"
    }

}
