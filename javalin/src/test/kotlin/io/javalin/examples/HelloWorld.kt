/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin

fun main() {
    val app = Javalin.create { config ->
        config.routes.get("/") { ctx -> ctx.result("Hello World") }
    }.start(7070)
}
