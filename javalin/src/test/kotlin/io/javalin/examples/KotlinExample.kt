/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.HttpStatus
import io.javalin.testing.TypedException

fun main() {

    Javalin.create().apply {

        get("/users") { ctx ->
            ctx.result("")
        }

        post("/users/create") { ctx ->
            ctx.status(HttpStatus.CREATED)
        }

        patch("/users/update/:id") { ctx ->
            ctx.status(HttpStatus.NO_CONTENT)
        }

        delete("/users/delete/:id") { ctx ->
            ctx.status(HttpStatus.NO_CONTENT)
        }

        exception(Exception::class.java) { e, ctx ->
            e.printStackTrace()
        }

        exception(TypedException::class.java) { e, ctx ->
            e.proofOfType()
        }

        error(HttpStatus.NOT_FOUND) { ctx ->
            ctx.result("not found")
        }

    }.start(7070)

}

