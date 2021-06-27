/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.plugin.json.jsonMapper
import java.util.*
import javax.servlet.http.Cookie

@Suppress("UNCHECKED_CAST")
class CookieStore(val context: Context, cookie: String?) {

    companion object {
        var COOKIE_NAME = "javalin-cookie-store"
    }

    private val cookieMap = deserialize(cookie)

    fun serializeToCookie() = Cookie(COOKIE_NAME, serialize(cookieMap)).apply { path = "/" }

    operator fun <T> get(key: String) = cookieMap[key] as T

    operator fun set(key: String, value: Any) = cookieMap.put(key, value)

    fun clear() = cookieMap.clear()

    private fun deserialize(cookie: String?) = if (!cookie.isNullOrEmpty()) {
        context.jsonMapper().fromJson(String(Base64.getDecoder().decode(cookie)), Map::class.java) as MutableMap<String, Any>
    } else mutableMapOf()

    private fun serialize(map: MutableMap<String, Any>) = Base64.getEncoder().encodeToString(context.jsonMapper().toJson(map).toByteArray())
}
