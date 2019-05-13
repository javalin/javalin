/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.core.security.Role;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.core.security.SecurityUtil.roles;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_ONE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_THREE;
import static io.javalin.examples.HelloWorldAuth.MyRoles.ROLE_TWO;

public class HelloWorldAuth {

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.accessManager((handler, ctx, permittedRoles) -> {
                String userRole = ctx.queryParam("role");
                if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
                    handler.handle(ctx);
                } else {
                    ctx.status(401).result("Unauthorized");
                }
            });
        }).routes(() -> {
            get("/hello", ctx -> ctx.result("Hello World 1"), roles(ROLE_ONE));
            path("/api", () -> {
                get("/test", ctx -> ctx.result("Hello World 2"), roles(ROLE_TWO));
                get("/tast", ctx -> ctx.status(200).result("Hello world 3"), roles(ROLE_THREE));
                get("/hest", ctx -> ctx.status(200).result("Hello World 4"), roles(ROLE_ONE, ROLE_TWO));
                get("/hast", ctx -> ctx.status(200).result("Hello World 5").header("test", "tast"), roles(ROLE_ONE, ROLE_THREE));
            });
        }).start(7070);
    }

    enum MyRoles implements Role {
        ROLE_ONE, ROLE_TWO, ROLE_THREE
    }

}
