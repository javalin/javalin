/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.security.RouteRole;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_ONE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_THREE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_TWO;
import static io.javalin.http.HttpStatus.OK;
import static io.javalin.http.HttpStatus.UNAUTHORIZED;

public class HelloWorldAuth {

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.accessManager((handler, ctx, routeRoles) -> {
                String userRole = ctx.queryParam("role");
                if (userRole != null && routeRoles.contains(MyRoles.valueOf(userRole))) {
                    handler.handle(ctx);
                } else {
                    ctx.status(UNAUTHORIZED).result("Unauthorized");
                }
            });
        }).routes(() -> {
            get("/hello", ctx -> ctx.result("Hello World 1"), ROLE_ONE);
            path("/api", () -> {
                get("/test", ctx -> ctx.result("Hello World 2"), ROLE_TWO);
                get("/tast", ctx -> ctx.status(OK).result("Hello world 3"), ROLE_THREE);
                get("/hest", ctx -> ctx.status(OK).result("Hello World 4"), ROLE_ONE, ROLE_TWO);
                get("/hast", ctx -> ctx.status(OK).result("Hello World 5").header("test", "tast"), ROLE_ONE, ROLE_THREE);
            });
        }).start(7070);
    }

    enum MyRoles implements RouteRole {
        ROLE_ONE, ROLE_TWO, ROLE_THREE
    }

}
