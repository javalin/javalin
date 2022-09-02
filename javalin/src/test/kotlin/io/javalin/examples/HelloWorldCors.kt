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

    val corsApp = Javalin.create {
        it.plugins.enableCors {cors ->
            cors.add {
                it.allowHost("http://localhost:7001/", "http://localhost:7002")
            }
        }
    }.start(7070)

    corsApp.routes {
        get { it.json("Hello Get") }
        post { it.json("Hello Post") }
        patch { it.json("Hello Patch") }
    }

    Javalin.create().start(7001).get("/") { it.html("Try some CORS") }
    Javalin.create().start(7002).get("/") { it.html("Try some CORS") }
    Javalin.create().start(7003).get("/") { it.html("No CORS here") }

}

