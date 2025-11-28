/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.examples.KRole.ROLE_ONE
import io.javalin.examples.KRole.ROLE_THREE
import io.javalin.examples.KRole.ROLE_TWO
import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import io.javalin.security.RouteRole

enum class KRole : RouteRole {
    ROLE_ONE, ROLE_TWO, ROLE_THREE
}

fun main() {
    Javalin.create { cfg ->
        cfg.routes.beforeMatched { ctx ->
            val userRole = ctx.queryParam("role")?.let { KRole.valueOf(it) } ?: throw UnauthorizedResponse()
            val routeRoles = ctx.routeRoles()
            if (userRole !in routeRoles) {
                throw UnauthorizedResponse()
            }
        }
        cfg.routes.apiBuilder {
            get("/hello", { it.result("Hello World 1") }, ROLE_ONE)
            path("/api") {
                get("/test", { it.result("Hello World 2") }, ROLE_TWO)
                get("/tast", { it.status(HttpStatus.OK).result("Hello world 3") }, ROLE_THREE)
                get("/hest", { it.status(HttpStatus.OK).result("Hello World 4") }, ROLE_ONE, ROLE_TWO)
                get("/hast", { it.status(HttpStatus.OK).result("Hello World 5").header("test", "tast") }, ROLE_ONE, ROLE_THREE)
            }
        }
    }.start(7070)
}
