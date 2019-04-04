/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.Handler
import io.javalin.core.HandlerType
import io.javalin.core.JavalinServlet
import io.javalin.security.CoreRoles

object CorsUtil {
    fun enableCorsForOrigin(servlet: JavalinServlet, origins: Array<String>) {
        if (origins.isEmpty()) throw IllegalArgumentException("Origins cannot be empty.")
        servlet.addHandler(HandlerType.BEFORE, "*", CorsBeforeHandler(origins), setOf())
        servlet.addHandler(HandlerType.OPTIONS, "*", CorsOptionsHandler(), setOf(CoreRoles.NO_WRAP))
    }
}


class CorsBeforeHandler(private val origins: Array<String>) : Handler {
    override fun handle(ctx: Context) {
        (ctx.header(Header.ORIGIN) ?: ctx.header(Header.REFERER))?.let { header ->
            origins.map { it.removeSuffix("/") }.firstOrNull { it == "*" || header.startsWith(it) }?.let {
                ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, header)
                ctx.header(Header.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
            }
        }
    }
}

class CorsOptionsHandler : Handler {
    override fun handle(ctx: Context) {
        ctx.header(Header.ACCESS_CONTROL_REQUEST_HEADERS)?.let {
            ctx.header(Header.ACCESS_CONTROL_ALLOW_HEADERS, it)
        }
        ctx.header(Header.ACCESS_CONTROL_REQUEST_METHOD)?.let {
            ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, it)
        }
    }
}
