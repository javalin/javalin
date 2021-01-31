/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler

class CorsBeforeHandler(private val origins: List<String>) : Handler {
    override fun handle(ctx: Context) {
        (ctx.header(Header.ORIGIN) ?: ctx.header(Header.REFERER))?.let { header ->
            origins.map { it.removeSuffix("/") }.firstOrNull { it == "*" || header == it }?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, header)
                ctx.header(Header.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
            }
        }

        if (ctx.method() == "OPTIONS") {
            ctx.header(Header.ACCESS_CONTROL_REQUEST_HEADERS)?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_HEADERS, it)
            }
            ctx.header(Header.ACCESS_CONTROL_REQUEST_METHOD)?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, it)
            }
        }
    }
}
