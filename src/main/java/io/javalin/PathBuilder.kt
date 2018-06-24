/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.net.URLEncoder

class PathBuilder(private val baseUrl: String) {

    private val queryParams = mutableMapOf<String, Array<out String>>()
    private val pathParams = mutableMapOf<String, String>()

    fun pathParam(key: String, value: String): PathBuilder {
        if (!baseUrl.contains(key)) {
            throw IllegalArgumentException("No path-param matching '$key'")
        }
        if (!key.startsWith(":")) {
            throw IllegalArgumentException("Path-param must start with ':'")
        }
        pathParams[key] = value
        return this
    }

    fun queryParam(key: String, vararg value: String): PathBuilder {
        queryParams[key] = value
        return this
    }

    fun build() = if (queryParams.isEmpty()) buildPathString(baseUrl) else "${buildPathString(baseUrl)}?${buildQueryString()}"

    private fun buildPathString(baseUrl: String) = baseUrl.split("/").map {
        if (pathParams[it] != null) pathParams[it]!!.encoded() else it
    }.joinToString("/")

    private fun buildQueryString() = queryParams.map {
        it.key.encoded() + "=" + it.value.joinToString(",") { it.encoded() }
    }.joinToString("&")

    private fun String.encoded() = URLEncoder.encode(this, "UTF-8")

}

