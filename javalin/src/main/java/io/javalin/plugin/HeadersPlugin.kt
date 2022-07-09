/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin

class HeadersPlugin(private val headers: Headers) : Plugin {

    override fun apply(app: Javalin) {
        app.before { ctx ->
            headers.headers.forEach { (name, value) ->
                ctx.header(name, value)
            }
        }
    }

}
