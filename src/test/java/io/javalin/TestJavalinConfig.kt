/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import org.eclipse.jetty.server.Server
import org.junit.Test

class TestJavalinConfig {

    @Test
    fun `test Javalin#config()`() {

        val app = Javalin.create().config { c ->
            c.contextPath = "/"
            c.caseSensitiveUrls = false
            c.devLogging = true
            c.requestLogger { ctx, ms ->
                println("${ctx.method()} ${ctx.path()}, took $ms}")
            }
            c.server { Server() }
            c.addStaticFiles("/public")
            c.addSinglePageHandler("/", "/public/subpage/index.html")
        }.start()

        app.stop()

    }

}
