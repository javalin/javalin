/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;

import java.util.Set;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.examples.HelloWorldAuth.JRole.ROLE_ONE;
import static io.javalin.examples.HelloWorldAuth.JRole.ROLE_THREE;
import static io.javalin.examples.HelloWorldAuth.JRole.ROLE_TWO;
import static io.javalin.http.HttpStatus.OK;

public class HelloWorldAuth {

    enum JRole implements RouteRole {
        ROLE_ONE, ROLE_TWO, ROLE_THREE
    }

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.routes.beforeMatched(ctx -> {
                Set<RouteRole> routeRoles = ctx.routeRoles();
                String userRole = ctx.queryParam("role");
                if (userRole == null || !routeRoles.contains(JRole.valueOf(userRole))) {
                    throw new UnauthorizedResponse();
                }
            });
            config.routes.apiBuilder(() -> {
                get("/hello", ctx -> ctx.result("Hello World 1"), ROLE_ONE);
                path("/api", () -> {
                    get("/test", ctx -> ctx.result("Hello World 2"), ROLE_TWO);
                    get("/tast", ctx -> ctx.status(OK).result("Hello world 3"), ROLE_THREE);
                    get("/hest", ctx -> ctx.status(OK).result("Hello World 4"), ROLE_ONE, ROLE_TWO);
                    get("/hast", ctx -> ctx.status(OK).result("Hello World 5").header("test", "tast"), ROLE_ONE, ROLE_THREE);
                });
            });
        }).start(7070);
    }

}
