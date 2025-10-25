/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.patch
import io.javalin.apibuilder.ApiBuilder.post

fun main() {
    Javalin.create { cfg ->
        cfg.routes.apiBuilder {
            get { it.json("Hello Get") }
            post { it.json("Hello Post") }
            patch { it.json("Hello Patch") }
        }
    }.start(7070)

    Javalin.create { it.routes.get("/") { it.html("Try some CORS") } }.start(7001)
    Javalin.create { it.routes.get("/") { it.html("Try some CORS") } }.start(7002)
    Javalin.create { it.routes.get("/") { it.html("No CORS here") } }.start(7003)
}
