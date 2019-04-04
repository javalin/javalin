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
import io.javalin.core.HandlerMetaInfo;
import io.javalin.core.HandlerType;
import io.javalin.core.JavalinEvent;
import io.javalin.core.JavalinServer;
import io.javalin.core.JavalinServerConfig;
import io.javalin.core.JavalinServlet;
import io.javalin.core.JavalinServletConfig;
import io.javalin.core.util.LogUtil;
import io.javalin.core.util.RouteOverviewRenderer;
import io.javalin.core.util.Util;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import io.javalin.serversentevent.SseClient;
import io.javalin.serversentevent.SseHandler;
import io.javalin.websocket.JavalinWsServlet;
import io.javalin.websocket.WsHandler;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Javalin {

    public static Logger log = LoggerFactory.getLogger(Javalin.class);

    protected boolean showJavalinBanner = true;

    protected Map<Class<?>, Object> appAttributes = new HashMap<>();

    protected JavalinServer server = new JavalinServer();
    protected JavalinServlet servlet = new JavalinServlet(appAttributes);
    protected JavalinWsServlet wsServlet = new JavalinWsServlet();

    protected EventManager eventManager = new EventManager(this);
    public EventAttacher on = eventManager.getEventAttacher();

    protected Javalin() {
        Util.logJavalinBanner(showJavalinBanner);
        Util.logWarningIfNotStartedAfterOneSecond(this.server);
    }

    /**
     * Creates an instance of the application for further configuration.
     * The server does not run until {@link Javalin#start()} is called.
     *
     * @return instance of application for configuration.
     * @see Javalin#start()
     * @see Javalin#start(int)
     */
    public static Javalin create() {
        return new Javalin();
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
        server.getConfig().setPort(port);
        return this.start();
    }

    /**
     * Synchronously starts the application instance.
     *
     * @return running application instance.
     * @see Javalin#create()
     */
    public Javalin start() {
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
            if (e.getMessage() != null && e.getMessage().contains("Failed to bind to")) {
                throw new RuntimeException("Port already in use. Make sure no other process is using port " + server.getConfig().getPort() + " and try again.", e);
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
            server.getConfig().getServer().stop();
        } catch (Exception e) {
            log.error("Javalin failed to stop gracefully", e);
        }
        log.info("Javalin has stopped");
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPED);
        return this;
    }

    /**
     * Configure instance to not show banner in logs.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin disableStartupBanner() {
        showJavalinBanner = false;
        return this;
    }

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    public int port() {
        return server.getConfig().getPort();
    }

    /**
     * Configure instance to log debug information for each request.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableDevLogging() {
        servlet.getConfig().setRequestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
        return this;
    }

    /**
     * Configure instance to display a visual overview of all its mapped routes
     * on the specified path.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableRouteOverview(@NotNull String path) {
        return enableRouteOverview(path, new HashSet<>());
    }

    /**
     * Configure instance to display a visual overview of all its mapped routes
     * on the specified path with the specified roles
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableRouteOverview(@NotNull String path, @NotNull Set<Role> permittedRoles) {
        if (this.servlet.getMatcher().hasEntries()) {
            throw new IllegalStateException("The route-overview listens for 'onHandlerAdded' events. It must be enabled before adding handlers.");
        }
        return this.get(path, new RouteOverviewRenderer(this), permittedRoles);
    }

    /**
     * Registers an attribute on the instance.
     * Instance is available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class, myExtInstance())
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin attribute(Class clazz, Object obj) {
        appAttributes.put(clazz, obj);
        return this;
    }

    /**
     * Retrieve an attribute stored on the instance.
     * Available on the {@link Context} through {@link Context#appAttribute}.
     * Ex: app.attribute(MyExt.class).myMethod()
     * Ex: ctx.appAttribute(MyExt.class).myMethod()
     */
    @SuppressWarnings("unchecked")
    public <T> T attribute(Class<T> clazz) {
        return (T) appAttributes.get(clazz);
    }

    /**
     * Adds an exception mapper to the instance.
     * Useful for turning exceptions into standardized errors/messages/pages
     *
     * @see <a href="https://javalin.io/documentation#exception-mapping">Exception mapping in docs</a>
     */
    public <T extends Exception> Javalin exception(@NotNull Class<T> exceptionClass, @NotNull ExceptionHandler<? super T> exceptionHandler) {
        servlet.getExceptionMapper().getExceptionMap().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
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
        eventManager.fireHandlerAddedEvent(new HandlerMetaInfo(handlerType, Util.prefixContextPath(server.getConfig().getContextPath(), path), handler, roles));
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

    // ********************************************************************************************
    // HTTP verbs
    // ********************************************************************************************

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
     * Adds a TRACE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin trace(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.TRACE, path, handler);
    }

    /**
     * Adds a CONNECT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin connect(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.CONNECT, path, handler);
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
     * Adds a TRACE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin trace(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.TRACE, path, handler, permittedRoles);
    }

    /**
     * Adds a CONNECT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin connect(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        return addHandler(HandlerType.CONNECT, path, handler, permittedRoles);
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
     * Adds a WebSocket handler on the specified path.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Consumer<WsHandler> ws) {
        wsServlet.addHandler(path, ws);
        eventManager.fireHandlerAddedEvent(new HandlerMetaInfo(HandlerType.WEBSOCKET, Util.prefixContextPath(server.getConfig().getContextPath(), path), ws, new HashSet<>()));
        return this;
    }

    /**
     * Configures a web socket handler to be called after every web socket event
     * Will override the default logger of {@link Javalin#enableDevLogging()}.
     */
    public Javalin wsLogger(@NotNull Consumer<WsHandler> ws) {
        wsServlet.setWsLogger(ws);
        return this;
    }

    /**
     * Configure the WebSocketServletFactory of the instance
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        wsServlet.setWsFactoryConfig(wsFactoryConfig);
        return this;
    }

    // ********************************************************************************************
    // Servlet and Server configuration
    // ********************************************************************************************

    public Javalin server(Consumer<JavalinServerConfig> server) {
        server.accept(this.server.getConfig());
        return this;
    }

    public Javalin servlet(Consumer<JavalinServletConfig> servlet) {
        servlet.accept(this.servlet.getConfig());
        return this;
    }

    public Javalin configure(BiConsumer<JavalinServletConfig, JavalinServerConfig> config) {
        config.accept(this.servlet.getConfig(), this.server.getConfig());
        return this;
    }

}
