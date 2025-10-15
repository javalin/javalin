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

    Javalin.create {

        it.routes.get("/users") { ctx ->
            ctx.result("")
        }

        it.routes.post("/users/create") { ctx ->
            ctx.status(HttpStatus.CREATED)
        }

        it.routes.patch("/users/update/:id") { ctx ->
            ctx.status(HttpStatus.NO_CONTENT)
        }

        it.routes.delete("/users/delete/:id") { ctx ->
            ctx.status(HttpStatus.NO_CONTENT)
        }

        it.routes.exception(Exception::class.java) { e, ctx ->
            e.printStackTrace()
        }

        it.routes.exception(TypedException::class.java) { e, ctx ->
            e.proofOfType()
        }

        it.routes.error(HttpStatus.NOT_FOUND) { ctx ->
            ctx.result("not found")
        }

    }.start(7070)

}
