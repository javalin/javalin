/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.testing.TypedException

fun main(args: Array<String>) {

    Javalin.create().apply {

        get("/users") { ctx ->
            ctx.result("")
        }

        post("/users/create") { ctx ->
            ctx.status(201)
        }

        patch("/users/update/:id") { ctx ->
            ctx.status(204)
        }

        delete("/users/delete/:id") { ctx ->
            ctx.status(204)
        }

        exception(Exception::class.java) { e, ctx ->
            e.printStackTrace()
        }

        exception(TypedException::class.java) { e, ctx ->
            e.proofOfType()
        }

        error(404) { ctx ->
            ctx.result("not found")
        }

    }.start(7070)

}

