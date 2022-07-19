/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.security.RouteRole
import io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_ONE
import io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_THREE
import io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_TWO
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HttpCode

enum class MyRoles : io.javalin.security.RouteRole {
    ROLE_ONE, ROLE_TWO, ROLE_THREE
}

fun main() {

    val app = Javalin.create { it.core.accessManager(::accessManager) }.start(7070)

    app.routes {
        get("/hello", { it.result("Hello World 1") }, ROLE_ONE)
        path("/api") {
            get("/test", { it.result("Hello World 2") }, ROLE_TWO)
            get("/tast", { it.status(HttpCode.OK).result("Hello world 3") }, ROLE_THREE)
            get("/hest", { it.status(HttpCode.OK).result("Hello World 4") }, ROLE_ONE, ROLE_TWO)
            get("/hast", { it.status(HttpCode.OK).result("Hello World 5").header("test", "tast") }, ROLE_ONE, ROLE_THREE)
        }
    }

}

private fun accessManager(handler: Handler, ctx: Context, routeRoles: Set<io.javalin.security.RouteRole>) {
    val userRole = ctx.queryParam("role")
    if (userRole != null && routeRoles.contains(MyRoles.valueOf(userRole))) {
        handler.handle(ctx)
    } else {
        ctx.status(HttpCode.UNAUTHORIZED).result("Unauthorized")
    }
}

