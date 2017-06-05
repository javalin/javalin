/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.TypedException


fun main(args: Array<String>) {

    val app = Javalin.create().port(7000)

    with(app) {

        get("/users") { req, res ->
            res.body("");
        }

        post("/users/create") { req, res ->
            res.status(201)
        }

        patch("/users/update/:id") { req, res ->
            res.status(204)
        }

        delete("/users/delete/:id") { req, res ->
            res.status(204)
        }

        exception(Exception::class.java) { e, req, res ->
            e.printStackTrace()
        }

        exception(TypedException::class.java) { e, req, res ->
            e.proofOfType()
        }

        error(404) { req, res ->
            res.body("not found");
        };

    }

}

