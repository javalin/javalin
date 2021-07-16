/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin

fun main() {

    val app = Javalin.create().start(7070)

    app.get("/hello") { ctx -> ctx.result("Hello World") }
    app.path("/api") {
        it.get("/test") { ctx -> ctx.result("Hello World") }
        it.get("/tast") { ctx -> ctx.status(200).result("Hello world") }
        it.get("/hest") { ctx -> ctx.status(200).result("Hello World") }
        it.get("/hast") { ctx -> ctx.status(200).result("Hello World").header("test", "tast") }
    }

}

