/*
 * Javalin - https://javalin.io
 * Copyright 2021 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util;

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.http.Context

class HeadersPlugin(private val headers: Headers) : Plugin {

    override fun apply(app: Javalin) {
        app.before { ctx: Context -> headers.headers.forEach { (t, u) -> ctx.header(t, u) } }
    }
}
