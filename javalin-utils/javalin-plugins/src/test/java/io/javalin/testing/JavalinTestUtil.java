package io.javalin.testing;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.router.Endpoint;

public class JavalinTestUtil {

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

    public static Javalin delete(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.DELETE, path).handler(handler)
        );
        return app;
    }

    public static Javalin patch(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.PATCH, path).handler(handler)
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

    public static Javalin before(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.BEFORE, path).handler(handler)
        );
        return app;
    }

    public static Javalin after(Javalin app, String path, Handler handler) {
        app.unsafe.pvt.internalRouter.addHttpEndpoint(
            Endpoint.create(HandlerType.AFTER, path).handler(handler)
        );
        return app;
    }
}

