/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Javalin;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.websocket.WsConfig;
import org.jetbrains.annotations.NotNull;

import static io.javalin.TestAccessManager.MyRoles.ROLE_ONE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_THREE;
import static io.javalin.TestAccessManager.MyRoles.ROLE_TWO;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class VisualTest {

    private static final Handler lambdaField = ctx -> {};

    public static void main(String[] args) {
        Javalin app = Javalin.create((config) -> {
            config.routing.contextPath = "/context-path";
            config.plugins.enableCors(cors -> cors.add(corsConfig -> corsConfig.reflectClientOrigin = true));
            config.plugins.enableRouteOverview("/route-overview");
        }).start();
        setupJavalinRoutes(app);
    }

    static void setupJavalinRoutes(Javalin app) {
        app.get("/", ctx -> ctx.redirect("/context-path/route-overview"))
            .get("/just-some-path", new HandlerImplementation())
            .post("/test/{hmm}/", VisualTest::methodReference)
            .put("/user/*", ctx -> ctx.result(""), ROLE_ONE)
            .get("/nonsense-paths/{test}", VisualTest.lambdaField, ROLE_ONE, ROLE_THREE)
            .delete("/just-words", VisualTest::methodReference, ROLE_ONE, ROLE_TWO)
            .before("*", VisualTest.lambdaField)
            .after("*", VisualTest.lambdaField)
            .head("/check/the/head", VisualTest::methodReference)
            .get("/{path1}/{path2}", VisualTest.lambdaField)
            .post("/user/create", VisualTest::methodReference, ROLE_ONE, ROLE_TWO)
            .put("/user/{user-id}", VisualTest.lambdaField)
            .patch("/patchy-mcpatchface", new ImplementingClass(), ROLE_ONE, ROLE_TWO)
            .delete("/users/{user-id}", new HandlerImplementation())
            .options("/what/{are}/*/my-options", new HandlerImplementation())
            .options("/what/{are}/*/my-options2", new HandlerImplementation(), ROLE_ONE, ROLE_TWO)
            .wsBefore(VisualTest::wsMethodRef)
            .ws("/websocket", VisualTest::wsMethodRef)
            .wsAfter("/my-path", VisualTest::wsMethodRef)
            .addHandler(HandlerType.CONNECT, "/test", VisualTest.lambdaField)
            .addHandler(HandlerType.TRACE, "/tracer", new HandlerImplementation())
            .addHandler(HandlerType.CONNECT, "/test2", VisualTest.lambdaField, ROLE_ONE, ROLE_TWO)
            .addHandler(HandlerType.TRACE, "/tracer2", new HandlerImplementation(), ROLE_ONE, ROLE_TWO)
            .sse("/sse", sse -> {
            });

        app.routes(() -> {
            path("users", () -> {
                get(new HandlerImplementation());
                post(new HandlerImplementation());
                path("{id}", () -> {
                    get(new HandlerImplementation());
                    patch(new HandlerImplementation());
                    delete(new HandlerImplementation());
                });
            });
            crud("/movies/{movie-id}", new CrudHandlerImpl());
        });
    }

    private static void wsMethodRef(WsConfig wsConfig) {
        wsConfig.onConnect(ctx -> ctx.session.getRemote().sendString("Connected!"));
    }

    private static void methodReference(Context context) {
    }

    private static class ImplementingClass implements Handler {
        @Override
        public void handle(@NotNull Context context) {
        }
    }

    public static class HandlerImplementation implements Handler {
        @Override
        public void handle(@NotNull Context context) {
        }
    }

    static class CrudHandlerImpl implements CrudHandler {
        @Override
        public void getAll(@NotNull Context ctx) {
        }
        @Override
        public void getOne(@NotNull Context ctx, @NotNull String resourceId) {
        }
        @Override
        public void create(@NotNull Context ctx) {
        }
        @Override
        public void update(@NotNull Context ctx, @NotNull String resourceId) {
        }
        @Override
        public void delete(@NotNull Context ctx, @NotNull String resourceId) {
        }
    }

}
