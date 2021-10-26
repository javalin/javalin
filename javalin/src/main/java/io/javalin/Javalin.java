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
import io.javalin.core.event.EventListener;
import io.javalin.core.event.EventManager;
import io.javalin.core.event.HandlerMetaInfo;
import io.javalin.core.event.JavalinEvent;
import io.javalin.core.event.WsHandlerMetaInfo;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.RouteRole;
import io.javalin.core.util.JavalinBindException;
import io.javalin.core.util.JavalinException;
import io.javalin.core.util.JavalinLogger;
import io.javalin.core.util.Util;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.Context;
import io.javalin.http.ErrorMapperKt;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.JavalinServlet;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.jetty.JavalinJettyServlet;
import io.javalin.jetty.JettyServer;
import io.javalin.jetty.JettyUtil;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsExceptionHandler;
import io.javalin.websocket.WsHandlerType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unchecked")
public class Javalin {

    /**
     * Do not use this field unless you know what you're doing.
     * Application config should be declared in {@link Javalin#create(Consumer)}
     */
    public JavalinConfig _conf = new JavalinConfig();

    protected JettyServer jettyServer; // null in standalone-mode
    protected JavalinJettyServlet javalinJettyServlet; // null in standalone-mode
    protected JavalinServlet javalinServlet = new JavalinServlet(_conf);

    protected EventManager eventManager = new EventManager();

    protected Javalin() {
        this.jettyServer = new JettyServer(_conf);
        this.javalinJettyServlet = new JavalinJettyServlet(_conf, javalinServlet);
    }

    public Javalin(JettyServer jettyServer, JavalinJettyServlet jettyServlet) {
        this.jettyServer = jettyServer;
        this.javalinJettyServlet = jettyServlet;
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
        JettyUtil.maybeLogIfServerNotStarted(app.jettyServer);
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
    public JavalinServlet javalinServlet() {
        return this.javalinServlet;
    }

    // Get the JavalinServer
    @Nullable
    public JettyServer jettyServer() {
        return this.jettyServer;
    }

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
        jettyServer.setServerHost(host);
        return start(port);
    }

    /**
     * Synchronously starts the application instance on the specified port.
     * Use port 0 to start the application instance on a random available port.
     *
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public Javalin start(int port) {
        jettyServer.setServerPort(port);
        return start();
    }

    /**
     * Synchronously starts the application instance on the configured port, or on
     * the configured ServerConnectors if the Jetty server has been manually configured.
     * If no port or connector is configured, the instance will start on port 8080.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
        Util.logJavalinBanner(this._conf.showJavalinBanner);
        JettyUtil.disableJettyLogger();
        long startupTimer = System.currentTimeMillis();
        if (jettyServer.started) {
            String message = "Server already started. If you are trying to call start() on an instance " +
                "of Javalin that was stopped using stop(), please create a new instance instead.";
            throw new IllegalStateException(message);
        }
        jettyServer.started = true;
        Util.printHelpfulMessageIfLoggerIsMissing();
        eventManager.fireEvent(JavalinEvent.SERVER_STARTING);
        try {
            if(this._conf.showJavalinStartupMessages) JavalinLogger.info("Starting Javalin ...");
            if(this._conf.showJavalinStartupMessages) Util.logJavalinVersion();
            jettyServer.start(javalinJettyServlet);
            if(this._conf.showJavalinStartupMessages) JavalinLogger.info("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/");
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED);
        } catch (Exception e) {
            JavalinLogger.error("Failed to start Javalin");
            eventManager.fireEvent(JavalinEvent.SERVER_START_FAILED);
            if (Boolean.TRUE.equals(jettyServer.server().getAttribute("is-default-server"))) {
                stop();// stop if server is default server; otherwise, the caller is responsible to stop
            }
            if (e.getMessage() != null && e.getMessage().contains("Failed to bind to")) {
                throw new JavalinBindException("Port already in use. Make sure no other process is using port " + Util.getPort(e) + " and try again.", e);
            } else if (e.getMessage() != null && e.getMessage().contains("Permission denied")) {
                throw new JavalinBindException("Port 1-1023 require elevated privileges (process must be started by admin).", e);
            }
            throw new JavalinException(e);
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
            jettyServer.server().stop();
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
        return jettyServer.getServerPort();
    }

    /**
     * Registers an attribute on the instance.
     * Instance is available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class, myExtInstance())
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin attribute(String key, Object value) {
        _conf.inner.appAttributes.put(key, value);
        return this;
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    public <T> T attribute(String key) {
        return (T) _conf.inner.appAttributes.get(key);
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
        try {
            endpointGroup.addEndpoints();
        } finally {
            ApiBuilder.clearStaticJavalin();
        }
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
        javalinServlet.getExceptionMapper().getHandlers().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
     */
    public Javalin error(int statusCode, @NotNull Handler handler) {
        javalinServlet.getErrorMapper().getErrorHandlerMap().put(statusCode, handler);
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
    public Javalin addHandler(@NotNull HandlerType handlerType, @NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        Set<RouteRole> roleSet = new HashSet<>(Arrays.asList(roles));
        javalinServlet.addHandler(handlerType, path, handler, roleSet);
        eventManager.fireHandlerAddedEvent(new HandlerMetaInfo(handlerType, Util.prefixContextPath(javalinServlet.getConfig().contextPath, path), handler, roleSet));
        return this;
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler) {
        return addHandler(httpMethod, path, handler, new RouteRole[0]); // no roles set for this route (open to everyone with default access manager)
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
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.GET, path, handler, roles);
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.POST, path, handler, roles);
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.PUT, path, handler, roles);
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.PATCH, path, handler, roles);
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.DELETE, path, handler, roles);
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.HEAD, path, handler, roles);
    }

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        return addHandler(HandlerType.OPTIONS, path, handler, roles);
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    public Javalin sse(@NotNull String path, @NotNull Consumer<SseClient> client) {
        return sse(path, client, new RouteRole[0]);
    }

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    public Javalin sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull RouteRole... roles) {
        return get(path, new SseHandler(client), roles);
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
        javalinJettyServlet.getWsExceptionMapper().getHandlers().put(exceptionClass, (WsExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    private Javalin addWsHandler(@NotNull WsHandlerType handlerType, @NotNull String path, @NotNull Consumer<WsConfig> wsConfig, @NotNull RouteRole... roles) {
        Set<RouteRole> roleSet = new HashSet<>(Arrays.asList(roles));
        javalinJettyServlet.addHandler(handlerType, path, wsConfig, roleSet);
        eventManager.fireWsHandlerAddedEvent(new WsHandlerMetaInfo(handlerType, Util.prefixContextPath(javalinServlet.getConfig().contextPath, path), wsConfig, roleSet));
        return this;
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     */
    private Javalin addWsHandler(@NotNull WsHandlerType handlerType, @NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        return addWsHandler(handlerType, path, wsConfig, new RouteRole[0]);
    }

    /**
     * Adds a WebSocket handler on the specified path.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsConfig> ws) {
        return ws(path, ws, new RouteRole[0]);
    }

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsConfig> ws, @NotNull RouteRole... roles) {
        return addWsHandler(WsHandlerType.WEBSOCKET, path, ws, roles);
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    public Javalin wsBefore(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        return addWsHandler(WsHandlerType.WS_BEFORE, path, wsConfig);
    }

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    public Javalin wsBefore(@NotNull Consumer<WsConfig> wsConfig) {
        return wsBefore("*", wsConfig);
    }

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    public Javalin wsAfter(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        return addWsHandler(WsHandlerType.WS_AFTER, path, wsConfig);
    }

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    public Javalin wsAfter(@NotNull Consumer<WsConfig> wsConfig) {
        return wsAfter("*", wsConfig);
    }

}
