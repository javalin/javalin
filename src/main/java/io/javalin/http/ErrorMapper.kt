/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.util.Header
import java.util.*

class ErrorMapper {
    val errorHandlerMap = HashMap<Int, Handler>()
    fun handle(statusCode: Int, ctx: Context) = errorHandlerMap[statusCode]?.handle(ctx)
}

fun contentTypeWrap(contentType: String, errorHandler: Handler) = Handler { ctx ->
    if (ctx.header(Header.ACCEPT)?.contains(contentType, ignoreCase = true) == true) {
        errorHandler.handle(ctx)
    }
}
