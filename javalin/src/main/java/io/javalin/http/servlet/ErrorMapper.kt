/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header

class ErrorMapper {

    data class MapperEntry(val statusCode: Int, val contentType: String, val handler: Handler)

    private val errorHandlers = mutableSetOf<MapperEntry>()

    fun addHandler(statusCode: Int, contentType: String, handler: Handler) =
        errorHandlers.add(MapperEntry(statusCode, contentType, handler))

    fun handle(statusCode: Int, ctx: Context) = errorHandlers.filter { it.statusCode == statusCode }.forEach {
        val contentTypeMatches by lazy { ctx.header(Header.ACCEPT)?.contains(it.contentType, ignoreCase = true) == true }
        if (it.contentType == "*" || contentTypeMatches) {
            it.handler.handle(ctx)
        }
    }

}
