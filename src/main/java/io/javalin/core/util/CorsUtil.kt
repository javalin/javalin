/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin

object CorsUtil {

    fun enableCors(app: Javalin, origins: Array<out String>): Javalin {
        if (origins.isEmpty()) throw IllegalArgumentException("Origins cannot be empty")
        app.options("*") { ctx ->
            ctx.header(Header.ACCESS_CONTROL_REQUEST_HEADERS)?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_HEADERS, it)
            }
            ctx.header(Header.ACCESS_CONTROL_REQUEST_METHOD)?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, it)
            }
        }
        app.before("*") { ctx ->
            (ctx.header(Header.ORIGIN) ?: ctx.header(Header.REFERER))?.let { header ->
                origins.map { it.removeSuffix("/") }.firstOrNull { it == "*" || header.startsWith(it) }?.let {
                    ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, header)
                }
            }
        }
        return app
    }

}
