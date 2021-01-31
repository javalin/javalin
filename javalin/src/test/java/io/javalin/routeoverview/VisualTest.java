/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Javalin;
import io.javalin.core.util.RouteOverviewPlugin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.websocket.WsHandler;
import static io.javalin.TestAccessManager.MyRoles.ROLE_ONE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_THREE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_TWO;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.core.security.SecurityUtil.roles;

public class VisualTest {

    private static Handler lambdaField = ctx -> {
    };

    public static void main(String[] args) {
        Javalin app = Javalin.create((config) -> {
            config.contextPath = "/context-path";
            config.enableCorsForAllOrigins();
            config.registerPlugin(new RouteOverviewPlugin("/route-overview"));
        }).start();

        app.get("/", ctx -> ctx.redirect("/context-path/route-overview"))
            .get("/just-some-path", new HandlerImplementation())
            .post("/test/:hmm/", VisualTest::methodReference)
            .put("/user/*", ctx -> ctx.result(""), roles(ROLE_ONE))
            .get("/nonsense-paths/:test", VisualTest.lambdaField, roles(ROLE_ONE, ROLE_THREE))
            .delete("/just-words", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO))
            .before("*", VisualTest.lambdaField)
            .after("*", VisualTest.lambdaField)
            .head("/check/the/head", VisualTest::methodReference)
            .get("/:path1/:path2", VisualTest.lambdaField)
            .post("/user/create", VisualTest::methodReference, roles(ROLE_ONE, ROLE_TWO))
            .put("/user/:user-id", VisualTest.lambdaField)
            .patch("/patchy-mcpatchface", new ImplementingClass(), roles(ROLE_ONE, ROLE_TWO))
            .delete("/users/:user-id", new HandlerImplementation())
            .options("/what/:are/*/my-options", new HandlerImplementation())
            .options("/what/:are/*/my-options2", new HandlerImplementation(), roles(ROLE_ONE, ROLE_TWO))
            .wsBefore(VisualTest::wsMethodRef)
            .ws("/websocket", VisualTest::wsMethodRef)
            .wsAfter("/my-path", VisualTest::wsMethodRef)
            .addHandler(HandlerType.CONNECT, "/test", VisualTest.lambdaField)
            .addHandler(HandlerType.TRACE, "/tracer", new HandlerImplementation())
            .addHandler(HandlerType.CONNECT, "/test2", VisualTest.lambdaField, roles(ROLE_ONE, ROLE_TWO))
            .addHandler(HandlerType.TRACE, "/tracer2", new HandlerImplementation(), roles(ROLE_ONE, ROLE_TWO))
            .sse("/sse", sse -> {
            });
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
            crud("/movies/:movie-id", new CrudHandlerImpl());
        });
    }

    private static void wsMethodRef(WsHandler wsHandler) {
        wsHandler.onConnect(ctx -> ctx.session.getRemote().sendString("Connected!"));
    }

    private static void methodReference(Context context) {
    }

    private static class ImplementingClass implements Handler {
        @Override
        public void handle(Context context) {
        }
    }

}
