/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.apibuilder.ApiBuilder;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.core.EventAttacher;
import io.javalin.core.EventManager;
import io.javalin.core.Extension;
import io.javalin.core.HandlerMetaInfo;
import io.javalin.core.JavalinConfig;
import io.javalin.core.JavalinEvent;
import io.javalin.core.JavalinServer;
import io.javalin.core.WsHandlerMetaInfo;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.CoreRoles;
import io.javalin.core.security.Role;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.RouteOverviewRenderer;
import io.javalin.core.util.Util;
import io.javalin.http.Context;
import io.javalin.http.ErrorHandler;
import io.javalin.http.ExceptionHandler;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.JavalinServlet;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.http.util.CorsBeforeHandler;
import io.javalin.http.util.CorsOptionsHandler;
import io.javalin.websocket.JavalinWsServlet;
import io.javalin.websocket.WsExceptionHandler;
import io.javalin.websocket.WsHandler;
import io.javalin.websocket.WsHandlerType;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.javalin.core.security.SecurityUtil.roles;

@SuppressWarnings("unchecked")
public class Javalin {

    public static Logger log = LoggerFactory.getLogger(Javalin.class);

    protected JavalinConfig config = new JavalinConfig();

    protected JavalinServer server = new JavalinServer(config);
    protected JavalinServlet servlet = new JavalinServlet(config);
    protected JavalinWsServlet wsServlet = new JavalinWsServlet(config);

    protected EventManager eventManager = new EventManager();

    protected Javalin() {
    }

    /**
     * Creates a new instance without any custom configuration.
     *
     * @see Javalin#create(Consumer)
     */
    public static Javalin create() {
        return create(c -> { // use defaults
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
        config.accept(app.config); // apply user config
        if (app.config.inner.routeOverview != null) {
            app.get(app.config.inner.routeOverview.getPath(), new RouteOverviewRenderer(app), app.config.inner.routeOverview.getRoles());
        }
        if (!app.config.inner.corsOrigins.isEmpty()) {
            app.before(new CorsBeforeHandler(app.config.inner.corsOrigins));
            app.options("*", new CorsOptionsHandler(), roles(CoreRoles.NO_WRAP));
        }
        if (app.config.enforceSsl) {
            app.before(SecurityUtil::sslRedirect);
        }
        if (app.config.showWarningIfNotStartedAfterOneSecond) {
            Util.logWarningIfNotStartedAfterOneSecond(app.server);
        }
        return app;
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
     * Synchronously starts the application instance.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
        Util.logJavalinBanner(this.config.showJavalinBanner);
        long startupTimer = System.currentTimeMillis();
        if (server.getStarted()) {
            throw new IllegalStateException("Cannot call start() again on a started server.");
        }
        Util.printHelpfulMessageIfLoggerIsMissing();
        eventManager.fireEvent(JavalinEvent.SERVER_STARTING);
        try {
            log.info("Starting Javalin ...");
            server.start(servlet, wsServlet);
            log.info("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/");
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED);
        } catch (Exception e) {
            log.error("Failed to start Javalin");
            eventManager.fireEvent(JavalinEvent.SERVER_START_FAILED);

            // only stop if Javalin instantiated default server; otherwise, the caller is responsible to stop
            Object isDefaultServer = server.server().getAttribute("is-default-server");
            if (isDefaultServer != null && Boolean.parseBoolean(isDefaultServer.toString())) {
                stop();
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
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPING);
        log.info("Stopping Javalin ...");
        try {
            server.server().stop();
        } catch (Exception e) {
            log.error("Javalin failed to stop gracefully", e);
        }
        log.info("Javalin has stopped");
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPED);
        return this;
    }

    public Javalin subscribe(Consumer<EventAttacher> eventAttacher) {
        EventAttacher attacher = new EventAttacher(this.eventManager);
        eventAttacher.accept(attacher);
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
     * Registers an {@link Extension} with the instance.
     * You're free to implement the extension as a class or a lambda expression
     */
    public Javalin register(Extension extension) {
        extension.registerOnJavalin(this);
        return this;
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
    public Javalin error(int statusCode, @NotNull ErrorHandler errorHandler) {
        servlet.getErrorMapper().getErrorHandlerMap().put(statusCode, errorHandler);
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
