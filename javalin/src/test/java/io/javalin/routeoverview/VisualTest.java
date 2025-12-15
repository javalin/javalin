/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routeoverview;

import io.javalin.Javalin;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.plugin.bundled.CorsPlugin;
import io.javalin.plugin.bundled.RouteOverviewPlugin;
import io.javalin.websocket.WsConfig;
import org.eclipse.jetty.websocket.api.Callback;
import org.jetbrains.annotations.NotNull;

import static io.javalin.TestBeforeMatched.MyRole.ROLE_ONE;
import static io.javalin.TestBeforeMatched.MyRole.ROLE_THREE;
import static io.javalin.TestBeforeMatched.MyRole.ROLE_TWO;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.testing.JavalinTestUtil.after;
import static io.javalin.testing.JavalinTestUtil.before;
import static io.javalin.testing.JavalinTestUtil.delete;
import static io.javalin.testing.JavalinTestUtil.get;
import static io.javalin.testing.JavalinTestUtil.head;
import static io.javalin.testing.JavalinTestUtil.options;
import static io.javalin.testing.JavalinTestUtil.patch;
import static io.javalin.testing.JavalinTestUtil.post;
import static io.javalin.testing.JavalinTestUtil.put;

public class VisualTest {

    private static final Handler lambdaField = ctx -> {};

    public static void main(String[] args) {
        Javalin app = Javalin.create((config) -> {
            config.router.contextPath = "/context-path";
            config.registerPlugin(new CorsPlugin(cors -> cors.addRule(corsConfig -> corsConfig.reflectClientOrigin = true)));
            config.registerPlugin(new RouteOverviewPlugin(cfg -> cfg.path = "/route-overview"));
        }).start();
        setupJavalinRoutes(app);
    }

    static void setupJavalinRoutes(Javalin app) {
        get(app, "/", ctx -> ctx.redirect("/context-path/route-overview"));
        get(app, "/just-some-path", new HandlerImplementation());
        post(app, "/test/{hmm}/", VisualTest::methodReference);
        put(app, "/user/*", ctx -> ctx.result(""), ROLE_ONE);
        get(app, "/nonsense-paths/{test}", VisualTest.lambdaField, ROLE_ONE, ROLE_THREE);
        delete(app, "/just-words", VisualTest::methodReference, ROLE_ONE, ROLE_TWO);
        before(app, "*", VisualTest.lambdaField);
        after(app, "*", VisualTest.lambdaField);
        head(app, "/check/the/head", VisualTest::methodReference);
        get(app, "/{path1}/{path2}", VisualTest.lambdaField);
        post(app, "/user/create", VisualTest::methodReference, ROLE_ONE, ROLE_TWO);
        put(app, "/user/{user-id}", VisualTest.lambdaField);
        patch(app, "/patchy-mcpatchface", new ImplementingClass(), ROLE_ONE, ROLE_TWO);
        delete(app, "/users/{user-id}", new HandlerImplementation());
        options(app, "/what/{are}/*/my-options", new HandlerImplementation());
        options(app, "/what/{are}/*/my-options2", new HandlerImplementation(), ROLE_ONE, ROLE_TWO);
        // TODO: Convert these WebSocket and SSE calls to new API
        // wsBefore(app, VisualTest::wsMethodRef);
        // ws(app, "/websocket", VisualTest::wsMethodRef);
        // wsAfter(app, "/my-path", VisualTest::wsMethodRef);
        // addHttpHandler(app, HandlerType.CONNECT, "/test", VisualTest.lambdaField);
        // addHttpHandler(app, HandlerType.TRACE, "/tracer", new HandlerImplementation());
        // addHttpHandler(app, HandlerType.CONNECT, "/test2", VisualTest.lambdaField, ROLE_ONE, ROLE_TWO);
        // addHttpHandler(app, HandlerType.TRACE, "/tracer2", new HandlerImplementation(), ROLE_ONE, ROLE_TWO);
        // sse(app, "/sse", sse -> { });

        app.unsafe.routes.apiBuilder(() -> {
            path("users", () -> {
                ApiBuilder.get(new HandlerImplementation());
                ApiBuilder.post(new HandlerImplementation());
                path("{id}", () -> {
                    ApiBuilder.get(new HandlerImplementation());
                    ApiBuilder.patch(new HandlerImplementation());
                    ApiBuilder.delete(new HandlerImplementation());
                });
            });
            crud("/movies/{movie-id}", new CrudHandlerImpl());
        });
    }

    private static void wsMethodRef(WsConfig wsConfig) {
        // Updated for Jetty 12 WebSocket API
        wsConfig.onConnect(ctx -> ctx.session.sendText("Connected!", Callback.NOOP));
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
