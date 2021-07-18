/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.core.security.RouteRole;

import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_ONE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_THREE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_TWO;

public class HelloWorldAuth {

    public static void main(String[] args) {
        Javalin.create(config -> config.accessManager((handler, ctx, routeRoles) -> {
            String userRole = ctx.queryParam("role");
            if (userRole != null && routeRoles.contains(MyRoles.valueOf(userRole))) {
                handler.handle(ctx);
            } else {
                ctx.status(401).result("Unauthorized");
            }
        }))
            .get("/hello", ctx -> ctx.result("Hello World 1"), ROLE_ONE)
            .path("/api", router -> {
                router.get("/test", ctx -> ctx.result("Hello World 2"), ROLE_TWO);
                router.get("/tast", ctx -> ctx.status(200).result("Hello world 3"), ROLE_THREE);
                router.get("/hest", ctx -> ctx.status(200).result("Hello World 4"), ROLE_ONE, ROLE_TWO);
                router.get("/hast", ctx -> ctx.status(200).result("Hello World 5").header("test", "tast"), ROLE_ONE, ROLE_THREE);
            })
            .start(7070);
    }

    enum MyRoles implements RouteRole {
        ROLE_ONE, ROLE_TWO, ROLE_THREE
    }

}
