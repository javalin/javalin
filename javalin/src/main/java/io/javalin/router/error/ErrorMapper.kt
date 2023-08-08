/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.error

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header

class ErrorMapper {

    data class MapperEntry(val statusCode: Int, val contentType: String, val handler: Handler)

    private val errorHandlers = mutableSetOf<MapperEntry>()

    fun addHandler(statusCode: Int, contentType: String, handler: Handler) {
        errorHandlers.add(MapperEntry(statusCode, contentType, handler))
    }

    fun handle(statusCode: Int, ctx: Context) {
        errorHandlers.asSequence()
            .filter { it.statusCode == statusCode }
            .filter { it.contentType == "*" || ctx.contentTypeMatches(it.contentType) }
            .forEach { it.handler.handle(ctx) }
    }

    private fun Context.contentTypeMatches(contentType: String): Boolean =
        header(Header.ACCEPT)?.contains(contentType, ignoreCase = true) == true

}
