/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin

object CorsUtil {

    fun enableCors(app: Javalin, origins: Array<String>): Javalin {
        app.options("*") { ctx ->
            ctx.header("Access-Control-Request-Headers")?.let {
                ctx.header("Access-Control-Allow-Headers", it)
            }
            ctx.header("Access-Control-Request-Method")?.let {
                ctx.header("Access-Control-Allow-Methods", it)
            }
        }
        app.before("*") { ctx ->
            val header = ctx.header("Origin") ?: ctx.header("Referer") ?: "NOT_AVAILABLE"
            origins.map { it.removeSuffix("/") }.firstOrNull { header.startsWith(it) }?.let {
                ctx.header("Access-Control-Allow-Origin", it)
            }
        }
        return app;
    }

}
