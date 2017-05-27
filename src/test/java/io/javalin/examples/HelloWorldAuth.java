/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.ApiBuilder;
import io.javalin.Javalin;
import io.javalin.security.Role;

import static io.javalin.examples.HelloWorldAuth.MyRoles.*;

public class HelloWorldAuth {

    enum MyRoles implements Role {
        ROLE_ONE, ROLE_TWO, ROLE_THREE;
    }

    public static void main(String[] args) {
        Javalin.create()
            .port(7070)
            .accessManager((handler, request, response, permittedRoles) -> {
                String userRole = request.queryParam("role");
                if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
                    handler.handle(request, response);
                } else {
                    response.status(401).body("Unauthorized");
                }
            })
            .routes(() -> {
                ApiBuilder.get("/hello", (req, res) -> res.body("Hello World 1"), Role.roles(ROLE_ONE));
                ApiBuilder.path("/api", () -> {
                    ApiBuilder.get("/test", (req, res) -> res.body("Hello World 2"), Role.roles(ROLE_TWO));
                    ApiBuilder.get("/tast", (req, res) -> res.status(200).body("Hello world 3"), Role.roles(ROLE_THREE));
                    ApiBuilder.get("/hest", (req, res) -> res.status(200).body("Hello World 4"), Role.roles(ROLE_ONE, ROLE_TWO));
                    ApiBuilder.get("/hast", (req, res) -> res.status(200).body("Hello World 5").header("test", "tast"), Role.roles(ROLE_ONE, ROLE_THREE));
                });
            });
    }

}
