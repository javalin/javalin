/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.ApiBuilder.get
import io.javalin.ApiBuilder.path
import io.javalin.Javalin

fun main(args: Array<String>) {

    val app = Javalin.create().start(7070)

    app.routes {
        get("/hello") { ctx -> ctx.result("Hello World") }
        path("/api") {
            get("/test") { ctx -> ctx.result("Hello World") }
            get("/tast") { ctx -> ctx.status(200).result("Hello world") }
            get("/hest") { ctx -> ctx.status(200).result("Hello World") }
            get("/hast") { ctx -> ctx.status(200).result("Hello World").header("test", "tast") }
        }
    }

}

