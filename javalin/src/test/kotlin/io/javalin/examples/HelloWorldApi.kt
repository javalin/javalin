/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.HttpCodes


fun main() {

    val app = Javalin.create().start(7070)

    app.routes {
        get("/hello") { it.result("Hello World") }
        path("/api") {
            get("/test") { it.result("Hello World") }
            get("/tast") { it.status(HttpCodes.OK).result("Hello world") }
            get("/hest") { it.status(HttpCodes.OK).result("Hello World") }
            get("/hast") { it.status(HttpCodes.OK).result("Hello World").header("test", "tast") }
        }
    }

}

