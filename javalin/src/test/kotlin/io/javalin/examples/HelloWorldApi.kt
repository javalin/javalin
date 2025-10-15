/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.HttpStatus


fun main() {
    Javalin.create {
        it.routes.apiBuilder {
            get("/hello") { it.result("Hello World") }
            path("/api") {
                get("/test") { it.result("Hello World") }
                get("/tast") { it.status(HttpStatus.OK).result("Hello world") }
                get("/hest") { it.status(HttpStatus.OK).result("Hello World") }
                get("/hast") { it.status(HttpStatus.OK).result("Hello World").header("test", "tast") }
            }
        }
    }.start(7070)
}

