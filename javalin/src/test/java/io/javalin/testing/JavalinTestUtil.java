/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.router.Endpoint;
import io.javalin.security.RouteRole;
import io.javalin.security.Roles;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsHandlerType;

import java.util.function.Consumer;

/**
 * Java utility class providing static methods for test routing.
 * This allows Java test files to maintain the app.verb() syntax.
 */
public class JavalinTestUtil {

    // HTTP Methods
    public static Javalin get(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.GET, path).handler(handler)
        );
        return app;
    }

    public static Javalin post(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.POST, path).handler(handler)
        );
        return app;
    }

    public static Javalin put(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.PUT, path).handler(handler)
        );
        return app;
    }

    public static Javalin put(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.PUT, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    public static Javalin patch(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.PATCH, path).handler(handler)
        );
        return app;
    }

    public static Javalin patch(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.PATCH, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    public static Javalin delete(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.DELETE, path).handler(handler)
        );
        return app;
    }

    public static Javalin delete(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.DELETE, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    public static Javalin head(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.HEAD, path).handler(handler)
        );
        return app;
    }

    public static Javalin options(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.OPTIONS, path).handler(handler)
        );
        return app;
    }

    public static Javalin options(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.OPTIONS, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    // HTTP Methods with roles
    public static Javalin get(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.GET, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    public static Javalin post(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.POST, path)
                .addMetadata(new Roles(java.util.Set.copyOf(java.util.Arrays.asList(roles))))
                .handler(handler)
        );
        return app;
    }

    // Before/After handlers
    public static Javalin before(Javalin app, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.BEFORE, "*").handler(handler)
        );
        return app;
    }

    public static Javalin before(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.BEFORE, path).handler(handler)
        );
        return app;
    }

    public static Javalin after(Javalin app, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.AFTER, "*").handler(handler)
        );
        return app;
    }

    public static Javalin after(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.AFTER, path).handler(handler)
        );
        return app;
    }

    // Exception handlers
    public static <E extends Exception> Javalin exception(Javalin app, Class<E> exceptionClass, io.javalin.http.ExceptionHandler<E> handler) {
        app.unsafe.pvt.internalRouter.addHttpExceptionHandler(exceptionClass, handler);
        return app;
    }

    // Error handlers
    public static Javalin error(Javalin app, int status, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpErrorHandler(status, "*", handler);
        return app;
    }

    // Server-Sent Events
    public static Javalin sse(Javalin app, String path, Consumer<SseClient> client) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.GET, path, java.util.Collections.emptySet(), new SseHandler(0, client))
        );
        return app;
    }

    // WebSocket handlers
    public static Javalin ws(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafe.pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET, path, wsConfig);
        return app;
    }

    // WebSocket exception handlers
    public static <E extends Exception> Javalin wsException(Javalin app, Class<E> exceptionClass, io.javalin.websocket.WsExceptionHandler<E> handler) {
        app.unsafe.pvt.internalRouter.addWsExceptionHandler(exceptionClass, handler);
        return app;
    }

    // WebSocket before/after handlers
    public static Javalin wsAfter(Javalin app, Consumer<WsConfig> wsConfig) {
        app.unsafe.pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_AFTER, "*", wsConfig);
        return app;
    }

    public static Javalin wsAfter(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafe.pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_AFTER, path, wsConfig);
        return app;
    }

    public static Javalin wsBefore(Javalin app, Consumer<WsConfig> wsConfig) {
        app.unsafe.pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_BEFORE, "*", wsConfig);
        return app;
    }

    public static Javalin wsBefore(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafe.pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_BEFORE, path, wsConfig);
        return app;
    }
}
