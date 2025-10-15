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
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.GET, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin post(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.POST, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin put(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.PUT, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin put(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.PUT, path, roles, handler)
        );
        return app;
    }

    public static Javalin patch(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.PATCH, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin patch(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.PATCH, path, roles, handler)
        );
        return app;
    }

    public static Javalin delete(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.DELETE, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin delete(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.DELETE, path, roles, handler)
        );
        return app;
    }

    public static Javalin head(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.HEAD, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin options(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.OPTIONS, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin options(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.OPTIONS, path, roles, handler)
        );
        return app;
    }

    // HTTP Methods with roles
    public static Javalin get(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.GET, path, roles, handler)
        );
        return app;
    }

    public static Javalin post(Javalin app, String path, Handler handler, RouteRole... roles) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.POST, path, roles, handler)
        );
        return app;
    }

    // Before/After handlers
    public static Javalin before(Javalin app, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.BEFORE, "*", java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin before(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.BEFORE, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin after(Javalin app, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.AFTER, "*", java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    public static Javalin after(Javalin app, String path, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.AFTER, path, java.util.Collections.emptySet(), handler)
        );
        return app;
    }

    // Exception handlers
    public static <E extends Exception> Javalin exception(Javalin app, Class<E> exceptionClass, io.javalin.http.ExceptionHandler<E> handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpExceptionHandler(exceptionClass, handler);
        return app;
    }

    // Error handlers
    public static Javalin error(Javalin app, int status, Handler handler) {
        app.unsafeConfig().pvt.internalRouter.addHttpErrorHandler(status, "*", handler);
        return app;
    }

    // Server-Sent Events
    public static Javalin sse(Javalin app, String path, Consumer<SseClient> client) {
        app.unsafeConfig().pvt.internalRouter.addHttpEndpoint(
            new Endpoint(HandlerType.GET, path, java.util.Collections.emptySet(), new SseHandler(0, client))
        );
        return app;
    }

    // WebSocket handlers
    public static Javalin ws(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafeConfig().pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET, path, wsConfig);
        return app;
    }

    // WebSocket exception handlers
    public static <E extends Exception> Javalin wsException(Javalin app, Class<E> exceptionClass, io.javalin.websocket.WsExceptionHandler<E> handler) {
        app.unsafeConfig().pvt.internalRouter.addWsExceptionHandler(exceptionClass, handler);
        return app;
    }

    // WebSocket before/after handlers
    public static Javalin wsAfter(Javalin app, Consumer<WsConfig> wsConfig) {
        app.unsafeConfig().pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_AFTER, "*", wsConfig);
        return app;
    }

    public static Javalin wsAfter(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafeConfig().pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_AFTER, path, wsConfig);
        return app;
    }

    public static Javalin wsBefore(Javalin app, Consumer<WsConfig> wsConfig) {
        app.unsafeConfig().pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_BEFORE, "*", wsConfig);
        return app;
    }

    public static Javalin wsBefore(Javalin app, String path, Consumer<WsConfig> wsConfig) {
        app.unsafeConfig().pvt.internalRouter.addWsHandler(WsHandlerType.WEBSOCKET_BEFORE, path, wsConfig);
        return app;
    }
}
