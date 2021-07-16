/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.core.JavalinConfig;
import io.javalin.core.JavalinServer;
import io.javalin.core.JettyUtil;
import io.javalin.core.event.EventListener;
import io.javalin.core.event.EventManager;
import io.javalin.core.event.JavalinEvent;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.core.util.JavalinLogger;
import io.javalin.core.util.Util;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.Context;
import io.javalin.http.ErrorMapperKt;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.JavalinServlet;
import io.javalin.http.Router;
import io.javalin.http.RouterContext;
import io.javalin.http.SubRouter;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.websocket.JavalinWsServlet;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsExceptionHandler;
import io.javalin.websocket.WsHandlerType;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public final class Javalin extends Router<Javalin> {

    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in {@link Javalin#create(Consumer)}
     */
    public JavalinConfig _conf = new JavalinConfig();

    protected JavalinServer server; // null in standalone-mode
    protected JavalinWsServlet wsServlet; // null in standalone-mode
    protected JavalinServlet servlet = new JavalinServlet(_conf);

    protected EventManager eventManager = new EventManager();

    private final RouterContext routerContext;

    protected Javalin() {
        this.server = new JavalinServer(_conf);
        this.wsServlet = new JavalinWsServlet(_conf, servlet);
        this.routerContext = new RouterContext(servlet, wsServlet, eventManager);
    }

    public Javalin(JavalinServer server, JavalinWsServlet wsServlet) {
        this.server = server;
        this.wsServlet = wsServlet;
        this.routerContext = new RouterContext(servlet, wsServlet, eventManager);
    }

    /**
     * Creates a new instance without any custom configuration.
     *
     * @see Javalin#create(Consumer)
     */
    public static Javalin create() {
        return create(config -> {
        });
    }

    /**
     * Creates a new instance with the user provided configuration.
     * The server does not run until {@link Javalin#start()} is called.
     *
     * @return application instance.
     * @see Javalin#start()
     * @see Javalin#start(int)
     */
    public static Javalin create(Consumer<JavalinConfig> config) {
        Javalin app = new Javalin();
        JavalinValidation.addValidationExceptionMapper(app);
        JavalinConfig.applyUserConfig(app, app._conf, config); // mutates app.config and app (adds http-handlers)
        if (app._conf.logIfServerNotStarted) {
            Util.logIfServerNotStarted(app.server);
        }
        return app;
    }

    // Create a standalone (non-jetty dependent) Javalin with the supplied config
    public static Javalin createStandalone(Consumer<JavalinConfig> config) {
        Javalin app = new Javalin(null, null);
        JavalinConfig.applyUserConfig(app, app._conf, config); // mutates app.config and app (adds http-handlers)
        return app;
    }

    // Create a standalone (non-jetty dependent) Javalin
    public static Javalin createStandalone() {
        return createStandalone(config -> {
        });
    }

    // Get JavalinServlet (for use in standalone mode)
    public JavalinServlet servlet() {
        return this.servlet;
    }

    public JavalinWsServlet wsServlet() {
        return wsServlet;
    }

    /**
     * Get the JavalinServer
     */
    // @formatter:off
    @Nullable
    public JavalinServer server() {
        return this.server;
    }
    // @formatter:off

    /**
     * Synchronously starts the application instance on the specified port
     * with the given host IP to bind to.
     *
     * @param host The host IP to bind to
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public Javalin start(String host, int port) {
        server.setServerHost(host);
        return start(port);
    }

    /**
     * Synchronously starts the application instance on the specified port.
     *
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public Javalin start(int port) {
        server.setServerPort(port);
        return start();
    }

    /**
     * Synchronously starts the application instance on the default port (7000).
     * To start on a random port use {@link Javalin#start(int)} with port 0.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
        Util.logJavalinBanner(this._conf.showJavalinBanner);
        JettyUtil.disableJettyLogger();
        long startupTimer = System.currentTimeMillis();
        if (server.getStarted()) {
            String message = "Server already started. If you are trying to call start() on an instance " +
                "of Javalin that was stopped using stop(), please create a new instance instead.";
            throw new IllegalStateException(message);
        }
        server.setStarted(true);
        Util.printHelpfulMessageIfLoggerIsMissing();
        eventManager.fireEvent(JavalinEvent.SERVER_STARTING);
        try {
            JavalinLogger.info("Starting Javalin ...");
            server.start(wsServlet);
            Util.logJavalinVersion();
            JavalinLogger.info("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/");
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED);
        } catch (Exception e) {
            JavalinLogger.error("Failed to start Javalin");
            eventManager.fireEvent(JavalinEvent.SERVER_START_FAILED);
            if (Boolean.TRUE.equals(server.server().getAttribute("is-default-server"))) {
                stop();// stop if server is default server; otherwise, the caller is responsible to stop
            }
            if (e.getMessage() != null && e.getMessage().contains("Failed to bind to")) {
                throw new RuntimeException("Port already in use. Make sure no other process is using port " + server.getServerPort() + " and try again.", e);
            } else if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                throw new RuntimeException("Port 1-1023 require elevated privileges (process must be started by admin).", e);
            }
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Synchronously stops the application instance.
     *
     * @return stopped application instance.
     */
    public Javalin stop() {
        JavalinLogger.info("Stopping Javalin ...");
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPING);
        try {
            server.server().stop();
        } catch (Exception e) {
            JavalinLogger.error("Javalin failed to stop gracefully", e);
        }
        JavalinLogger.info("Javalin has stopped");
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPED);
        return this;
    }

    public Javalin events(Consumer<EventListener> listener) {
        EventListener eventListener = new EventListener(this.eventManager);
        listener.accept(eventListener);
        return this;
    }

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    public int port() {
        return server.getServerPort();
    }

    /**
     * Registers an attribute on the instance.
     * Instance is available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class, myExtInstance())
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin attribute(Class<?> clazz, Object obj) {
        _conf.inner.appAttributes.put(clazz, obj);
        return this;
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    public <T> T attribute(Class<T> clazz) {
        return (T) _conf.inner.appAttributes.get(clazz);
    }

    // ********************************************************************************************
    // HTTP
    // ********************************************************************************************

    /**
     * Adds an exception mapper to the instance.
     *
     * @see <a href="https://javalin.io/documentation#exception-mapping">Exception mapping in docs</a>
     */
    public <T extends Exception> Javalin exception(@NotNull Class<T> exceptionClass, @NotNull ExceptionHandler<? super T> exceptionHandler) {
        servlet.getExceptionMapper().getHandlers().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
     */
    public Javalin error(int statusCode, @NotNull Handler handler) {
        servlet.getErrorMapper().getErrorHandlerMap().put(statusCode, handler);
        return this;
    }

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
     */
    public Javalin error(int statusCode, @NotNull String contentType, @NotNull Handler handler) {
        return error(statusCode, ErrorMapperKt.contentTypeWrap(contentType, handler));
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin addHandler(@NotNull HandlerType handlerType, @NotNull String path, @NotNull Handler handler, @NotNull Set<Role> roles) {
        routerContext.addHandler(handlerType, path, handler, roles);
        return this;
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler) {
        routerContext.addHandler(httpMethod, path, handler);
        return this;
    }

    @Override
    @NotNull
    public SubRouter path(@NotNull String path) {
        return new SubRouter(routerContext, path.startsWith("/") ? path : "/" + path);
    }

    // ********************************************************************************************
    // HTTP verbs
    // ********************************************************************************************

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.GET, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.POST, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.PUT, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.PATCH, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.DELETE, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.HEAD, path, handler, permittedRoles);
        return this;
    }

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.OPTIONS, path, handler, permittedRoles);
        return this;
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    @NotNull
    public Javalin sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull Set<Role> permittedRoles) {
        return get(path, new SseHandler(client), permittedRoles);
    }

    // ********************************************************************************************
    // Before/after handlers (filters)
    // ********************************************************************************************

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin before(@NotNull String path, @NotNull Handler handler) {
        routerContext.addHandler(HandlerType.BEFORE, path, handler);
        return this;
    }

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    @NotNull
    public Javalin after(@NotNull String path, @NotNull Handler handler) {
        routerContext.addHandler(HandlerType.AFTER, path, handler);
        return this;
    }

    // ********************************************************************************************
    // WebSocket
    // ********************************************************************************************

    /**
     * Adds a WebSocket exception mapper to the instance.
     *
     * @see <a href="https://javalin.io/documentation#exception-mapping">Exception mapping in docs</a>
     */
    public <T extends Exception> Javalin wsException(@NotNull Class<T> exceptionClass, @NotNull WsExceptionHandler<? super T> exceptionHandler) {
        wsServlet.getWsExceptionMapper().getHandlers().put(exceptionClass, (WsExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsConfig> ws, @NotNull Set<Role> permittedRoles) {
        routerContext.addWsHandler(WsHandlerType.WEBSOCKET, path, ws, permittedRoles);
        return this;
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    public Javalin wsBefore(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        routerContext.addWsHandler(WsHandlerType.WS_BEFORE, path, wsConfig);
        return this;
    }

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    public Javalin wsAfter(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        routerContext.addWsHandler(WsHandlerType.WS_AFTER, path, wsConfig);
        return this;
    }

}
