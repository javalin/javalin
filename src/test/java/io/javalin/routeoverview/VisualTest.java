/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Context;
import io.javalin.Handler;
import io.javalin.Javalin;
import io.javalin.misc.HandlerImplementation;
import io.javalin.websocket.WsHandler;
import static io.javalin.TestAccessManager.MyRoles.ROLE_ONE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_THREE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_TWO;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.security.SecurityUtil.roles;

public class VisualTest {

    private static Handler lambdaField = ctx -> {
    };

    public static void main(String[] args) {
        Javalin app = Javalin.create()
            .contextPath("/context-path")
            .enableRouteOverview("/route-overview")
            .enableCorsForAllOrigins();
        app.start();

        app.get("/", ctx -> ctx.redirect("/route-overview"));
        app.get("/just-some-path", new HandlerImplementation());
        app.post("/test/:hmm/", VisualTest::methodReference);
        app.put("/user/*", ctx -> ctx.result(""), roles(ROLE_ONE));
        app.get("/nonsense-paths/:test", VisualTest.lambdaField, roles(ROLE_ONE, ROLE_THREE));
        app.delete("/just-words", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.before("*", VisualTest.lambdaField);
        app.after("*", VisualTest.lambdaField);
        app.head("/check/the/head", VisualTest::methodReference);
        app.get("/:path1/:path2", VisualTest.lambdaField);
        app.post("/user/create", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO));
        app.put("/user/:user-id", VisualTest.lambdaField);
        app.patch("/patchy-mcpatchface", new ImplementingClass(), roles(ROLE_ONE, ROLE_TWO));
        app.delete("/users/:user-id", new HandlerImplementation());
        app.options("/what/:are/*/my-options", new HandlerImplementation());
        app.trace("/tracer", new HandlerImplementation());
        app.ws("/websocket", VisualTest::wsMethodRef);
        app.routes(() -> {
            path("users", () -> {
                get(new HandlerImplementation());
                post(new HandlerImplementation());
                path(":id", () -> {
                    get(new HandlerImplementation());
                    patch(new HandlerImplementation());
                    delete(new HandlerImplementation());
                });
            });
        });
    }

    private static void wsMethodRef(WsHandler wsHandler) {
        wsHandler.onConnect(session -> session.getRemote().sendString("Connected!"));
    }

    private static void methodReference(Context context) {
    }

    private static class ImplementingClass implements Handler {
        @Override
        public void handle(Context context) {
        }
    }

}
