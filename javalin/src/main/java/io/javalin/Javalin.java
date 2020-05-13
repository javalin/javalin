/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.core.JavalinConfig;
import io.javalin.core.JavalinServer;
import io.javalin.core.JettyUtil;
import io.javalin.core.event.EventListener;
import io.javalin.core.event.EventManager;
import io.javalin.core.event.HandlerMetaInfo;
import io.javalin.core.event.JavalinEvent;
import io.javalin.core.event.WsHandlerMetaInfo;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.core.util.Util;
import io.javalin.http.Context;
import io.javalin.http.ErrorMapperKt;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.JavalinServlet;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.websocket.JavalinWsServlet;
import io.javalin.websocket.WsExceptionHandler;
import io.javalin.websocket.WsHandler;
import io.javalin.websocket.WsHandlerType;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class Javalin {

    public static Logger log = LoggerFactory.getLogger(Javalin.class);

    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in {@link Javalin#create(Consumer)}
     */
    public JavalinConfig config = new JavalinConfig();

    protected JavalinServer server; // null in standalone-mode
    protected JavalinWsServlet wsServlet; // null in standalone-mode
    protected JavalinServlet servlet = new JavalinServlet(config);

    protected EventManager eventManager = new EventManager();

    protected Javalin() {
        this.server = new JavalinServer(config);
        this.wsServlet = new JavalinWsServlet(config, servlet);
    }

    public Javalin(JavalinServer server, JavalinWsServlet wsServlet) {
        this.server = server;
        this.wsServlet = wsServlet;
    }

    /**
     * Creates a new instance without any custom configuration.
     *
     * @see Javalin#create(Consumer)
     */
    public static Javalin create() {
        return create(JavalinConfig.noopConfig);
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
        JavalinConfig.applyUserConfig(app, app.config, config); // mutates app.config and app (adds http-handlers)
        if (app.config.logIfServerNotStarted) {
            Util.logIfServerNotStarted(app.server);
        }
        return app;
    }

    // Create a standalone (non-jetty dependent) Javalin with the supplied config
    public static Javalin createStandalone(Consumer<JavalinConfig> config) {
        Javalin app = new Javalin(null, null);
        JavalinConfig.applyUserConfig(app, app.config, config); // mutates app.config and app (adds http-handlers)
        return app;
    }

    // Create a standalone (non-jetty dependent) Javalin
    public static Javalin createStandalone() {
        return createStandalone(JavalinConfig.noopConfig);
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
    public @Nullable JavalinServer server() {
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
        Util.logJavalinBanner(this.config.showJavalinBanner);
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
            Javalin.log.info("Starting Javalin ...");
            server.start(wsServlet);
            Javalin.log.info("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/");
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED);
        } catch (Exception e) {
            Javalin.log.error("Failed to start Javalin");
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
        log.info("Stopping Javalin ...");
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPING);
        try {
            server.server().stop();
        } catch (Exception e) {
            log.error("Javalin failed to stop gracefully", e);
        }
        log.info("Javalin has stopped");
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
    public Javalin attribute(Class clazz, Object obj) {
        config.inner.appAttributes.put(clazz, obj);
        return this;
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    public <T> T attribute(Class<T> clazz) {
        return (T) config.inner.appAttributes.get(clazz);
    }

    /**
     * Creates a temporary static instance in the scope of the endpointGroup.
     * Allows you to call get(handler), post(handler), etc. without without using the instance prefix.
     *
     * @see <a href="https://javalin.io/documentation#handler-groups">Handler groups in documentation</a>
     * @see ApiBuilder
     */
    public Javalin routes(@NotNull EndpointGroup endpointGroup) {
        ApiBuilder.setStaticJavalin(this);
        endpointGroup.addEndpoints();
        ApiBuilder.clearStaticJavalin();
        return this;
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
        servlet.addHandler(handlerType, path, handler, roles);
        eventManager.fireHandlerAddedEvent(new HandlerMetaInfo(handlerType, Util.prefixContextPath(servlet.getConfig().contextPath, path), handler, roles));
        return this;
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler) {
        return addHandler(httpMethod, path, handler, new HashSet<>()); // no roles set for this route (open to everyone with default access manager)
    }

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin get(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.GET, path, handler);
    }

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.POST, path, handler);
    }

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PUT, path, handler);
    }

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PATCH, path, handler);
    }

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.DELETE, path, handler);
    }

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.HEAD, path, handler);
    }

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.OPTIONS, path, handler);
    }

    // ********************************************************************************************
    // Secured HTTP verbs
    // ********************************************************************************************

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.GET, path, handler, permittedRoles);
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.POST, path, handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.PUT, path, handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.PATCH, path, handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.DELETE, path, handler, permittedRoles);
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.HEAD, path, handler, permittedRoles);
    }

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.OPTIONS, path, handler, permittedRoles);
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    public Javalin sse(@NotNull String path, @NotNull Consumer<SseClient> client) {
        return sse(path, client, new HashSet<>());
    }

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
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
    public Javalin before(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.BEFORE, path, handler);
    }

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin before(@NotNull Handler handler) {
        return before("*", handler);
    }

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin after(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.AFTER, path, handler);
    }

    /**
     * Adds an AFTER request handler for all routes in the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin after(@NotNull Handler handler) {
        return after("*", handler);
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
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    private Javalin addWsHandler(@NotNull WsHandlerType handlerType, @NotNull String path, @NotNull Consumer<WsHandler> wsHandler, @NotNull Set<Role> roles) {
        wsServlet.addHandler(handlerType, path, wsHandler, roles);
        eventManager.fireWsHandlerAddedEvent(new WsHandlerMetaInfo(handlerType, Util.prefixContextPath(servlet.getConfig().contextPath, path), wsHandler, roles));
        return this;
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     */
    private Javalin addWsHandler(@NotNull WsHandlerType handlerType, @NotNull String path, @NotNull Consumer<WsHandler> wsHandler) {
        return addWsHandler(handlerType, path, wsHandler, new HashSet<>());
    }

    /**
     * Adds a WebSocket handler on the specified path.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsHandler> ws) {
        return ws(path, ws, new HashSet<>());
    }

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsHandler> ws, @NotNull Set<Role> permittedRoles) {
        return addWsHandler(WsHandlerType.WEBSOCKET, path, ws, permittedRoles);
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    public Javalin wsBefore(@NotNull String path, @NotNull Consumer<WsHandler> wsHandler) {
        return addWsHandler(WsHandlerType.WS_BEFORE, path, wsHandler);
    }

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    public Javalin wsBefore(@NotNull Consumer<WsHandler> wsHandler) {
        return wsBefore("*", wsHandler);
    }

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    public Javalin wsAfter(@NotNull String path, @NotNull Consumer<WsHandler> wsHandler) {
        return addWsHandler(WsHandlerType.WS_AFTER, path, wsHandler);
    }

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    public Javalin wsAfter(@NotNull Consumer<WsHandler> wsHandler) {
        return wsAfter("*", wsHandler);
    }

}
