/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.core.ErrorMapper;
import io.javalin.core.ExceptionMapper;
import io.javalin.core.HandlerEntry;
import io.javalin.core.HandlerType;
import io.javalin.core.JavalinServlet;
import io.javalin.core.PathMatcher;
import io.javalin.core.util.CorsUtil;
import io.javalin.core.util.Util;
import io.javalin.embeddedserver.EmbeddedServer;
import io.javalin.embeddedserver.EmbeddedServerFactory;
import io.javalin.embeddedserver.Location;
import io.javalin.embeddedserver.StaticFileConfig;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig;
import io.javalin.embeddedserver.jetty.websocket.WebSocketHandler;
import io.javalin.event.EventListener;
import io.javalin.event.EventManager;
import io.javalin.event.EventType;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Javalin {

    private static Logger log = LoggerFactory.getLogger(Javalin.class);

    private int port = 7000;
    private String contextPath = "/";
    private boolean dynamicGzipEnabled = false;

    private EmbeddedServer embeddedServer;
    private EmbeddedServerFactory embeddedServerFactory = new EmbeddedJettyFactory();

    private List<StaticFileConfig> staticFileConfig = new ArrayList<>();
    private PathMatcher pathMatcher = new PathMatcher();
    private ExceptionMapper exceptionMapper = new ExceptionMapper();
    private ErrorMapper errorMapper = new ErrorMapper();
    private LogLevel logLevel = LogLevel.OFF;
    private String defaultContentType = "text/plain";
    private String defaultCharacterEncoding = StandardCharsets.UTF_8.name();

    private EventManager eventManager = new EventManager();

    private AccessManager accessManager = (Handler handler, Context ctx, List<Role> permittedRoles) -> {
        throw new IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'");
    };

    private Javalin() {
    }

    /**
     * Creates an instance of the application for further configuration. The server does not run until {@link Javalin#start()} is called.
     *
     * @see Javalin#start()
     * @see Javalin#start(int)
     *
     * @return instance of application for configuration.
     */
    public static Javalin create() {
        Util.INSTANCE.printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond();
        return new Javalin();
    }

    /**
     * Creates and starts the application with default parameters on specified port.
     *
     * @param port to run on
     *
     * @see Javalin#create()
     * @see Javalin#start()
     *
     * @return running application instance.
     */
    public static Javalin start(int port) {
        return new Javalin()
            .port(port)
            .start();
    }

    // Begin embedded server methods

    private boolean started = false;

    /**
     * Synchronously starts an instance of the application.
     *
     * @see Javalin#create()
     *
     * @return running application instance.
     */
    public Javalin start() {
        if (!started) {
            log.info(Util.INSTANCE.javalinBanner());
            Util.INSTANCE.printHelpfulMessageIfLoggerIsMissing();
            Util.INSTANCE.setNoServerHasBeenStarted(false);
            eventManager.fireEvent(EventType.SERVER_STARTING, this);
            try {
                embeddedServer = embeddedServerFactory.create(new JavalinServlet(
                    contextPath,
                    pathMatcher,
                    exceptionMapper,
                    errorMapper,
                    pathWsHandlers,
                    logLevel,
                    dynamicGzipEnabled,
                    defaultContentType,
                    defaultCharacterEncoding
                ), staticFileConfig);
                log.info("Starting Javalin ...");
                port = embeddedServer.start(port);
                log.info("Javalin has started \\o/");
                started = true;
                eventManager.fireEvent(EventType.SERVER_STARTED, this);
            } catch (Exception e) {
                log.error("Failed to start Javalin", e);
                eventManager.fireEvent(EventType.SERVER_START_FAILED, this);
            }
        }
        return this;
    }

    /**
     * Synchronously stops application instance.
     *
     * @return stopped application instance.
     */
    public Javalin stop() {
        eventManager.fireEvent(EventType.SERVER_STOPPING, this);
        log.info("Stopping Javalin ...");
        try {
            embeddedServer.stop();
        } catch (Exception e) {
            log.error("Javalin failed to stop gracefully", e);
        }
        log.info("Javalin has stopped");
        eventManager.fireEvent(EventType.SERVER_STOPPED, this);
        return this;
    }

    /**
     * Treat '/test/' and '/test' as different URLs.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin dontIgnoreTrailingSlashes() {
        ensureActionIsPerformedBeforeServerStart("Telling Javalin to not ignore slashes");
        pathMatcher.setIgnoreTrailingSlashes(false);
        return this;
    }

    /**
     * Sets custom server implementation.
     *
     * @see <a href="https://javalin.io/documentation#custom-server">Documentation example</a>
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin embeddedServer(@NotNull EmbeddedServerFactory embeddedServerFactory) {
        ensureActionIsPerformedBeforeServerStart("Setting a custom server");
        this.embeddedServerFactory = embeddedServerFactory;
        return this;
    }

    /**
     * Serves static files from path in classpath.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#static-files>Static files in docs</a>
     */
    public Javalin enableStaticFiles(@NotNull String classpathPath) {
        return enableStaticFiles(classpathPath, Location.CLASSPATH);
    }

    /**
     * Serves static files from path in the given location.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#static-files>Static files in docs</a>
     */
    public Javalin enableStaticFiles(@NotNull String path, @NotNull Location location) {
        ensureActionIsPerformedBeforeServerStart("Enabling static files");
        staticFileConfig.add(new StaticFileConfig(path, location));
        return this;
    }

    /**
     * Context path (common prefix) for the instance.
     */
    public String contextPath() {
        return this.contextPath;
    }

    /**
     * Sets the context path (common prefix) for the instance.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin contextPath(@NotNull String contextPath) {
        ensureActionIsPerformedBeforeServerStart("Setting the context path");
        this.contextPath = Util.INSTANCE.normalizeContextPath(contextPath);
        return this;
    }

    /**
     * Port which is assigned to the instance.
     */
    public int port() {
        return port;
    }

    /**
     * Sets the port to run the instance on.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin port(int port) {
        ensureActionIsPerformedBeforeServerStart("Setting the port");
        this.port = port;
        return this;
    }

    /**
     * Sets request logger level to {@link LogLevel#STANDARD}.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableStandardRequestLogging() {
        return requestLogLevel(LogLevel.STANDARD);
    }

    /**
     * Sets request logger level to the given one.
     * 
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin requestLogLevel(@NotNull LogLevel logLevel) {
        ensureActionIsPerformedBeforeServerStart("Enabling request-logging");
        this.logLevel = logLevel;
        return this;
    }

    /**
     * Enables cross origin requests for defined origins.
     * 
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableCorsForOrigin(@NotNull String... origin) {
        ensureActionIsPerformedBeforeServerStart("Enabling CORS");
        return CorsUtil.INSTANCE.enableCors(this, origin);
    }

    /**
     * Enables cross origin requests for all origins.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableCorsForAllOrigins() {
        return enableCorsForOrigin("*");
    }

    /**
     * Enables dynamic gzip compression.
     *
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableDynamicGzip() {
        ensureActionIsPerformedBeforeServerStart("Enabling dynamic GZIP");
        this.dynamicGzipEnabled = true;
        return this;
    }

    public Javalin defaultContentType(String contentType) {
        ensureActionIsPerformedBeforeServerStart("Changing default content type");
        this.defaultContentType = contentType;
        return this;
    }

    public Javalin defaultCharacterEncoding(String characterEncoding) {
        ensureActionIsPerformedBeforeServerStart("Changing default character encoding");
        this.defaultCharacterEncoding = characterEncoding;
        return this;
    }

    private void ensureActionIsPerformedBeforeServerStart(@NotNull String action) {
        if (started) {
            throw new IllegalStateException(action + " must be done before starting the server");
        }
    }

    // End embedded server methods

    /**
     * Defines an access manager for the instance. Secured endpoints require one to be set.
     *
     * @see <a href="https://javalin.io/documentation#access-manager">Access manager in docs</a>
     */
    public Javalin accessManager(@NotNull AccessManager accessManager) {
        this.accessManager = accessManager;
        return this;
    }

    /**
     * Adds an exception mapper to the instance.
     *
     * @see <a href="https://javalin.io/documentation#exception-mapping">Exception mapping in docs</a>
     */
    public <T extends Exception> Javalin exception(@NotNull Class<T> exceptionClass, @NotNull ExceptionHandler<? super T> exceptionHandler) {
        exceptionMapper.getExceptionMap().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds a lifecycle event listener.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#lifecycle-events">Events in docs</a>
     */
    public Javalin event(@NotNull EventType eventType, @NotNull EventListener eventListener) {
        ensureActionIsPerformedBeforeServerStart("Event-mapping");
        eventManager.getListenerMap().get(eventType).add(eventListener);
        return this;
    }

    /**
     * Adds an error mapper to the instance.
     *
     * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
     */
    public Javalin error(int statusCode, @NotNull ErrorHandler errorHandler) {
        errorMapper.getErrorHandlerMap().put(statusCode, errorHandler);
        return this;
    }

    /**
     * Adds a group of handlers defined by ApiBuilder static methods.
     *
     * @see <a href="https://javalin.io/documentation#handler-groups">Handler groups in documentation</a>
     * @see ApiBuilder
     */
    public Javalin routes(@NotNull ApiBuilder.EndpointGroup endpointGroup) {
        ApiBuilder.setStaticJavalin(this);
        endpointGroup.addEndpoints();
        ApiBuilder.clearStaticJavalin();
        return this;
    }

    private Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler) {
        String prefixedPath = Util.INSTANCE.prefixContextPath(path, contextPath);
        pathMatcher.getHandlerEntries().add(new HandlerEntry(httpMethod, prefixedPath, handler));
        return this;
    }

    private Javalin addSecuredHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(httpMethod, path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    // HTTP verbs
    /**
     * Adds a GET request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin get(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.GET, path, handler);
    }

    /**
     * Adds a POST request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.POST, path, handler);
    }

    /**
     * Adds a PUT request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PUT, path, handler);
    }

    /**
     * Adds a PATCH request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PATCH, path, handler);
    }

    /**
     * Adds a DELETE request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.DELETE, path, handler);
    }

    /**
     * Adds a HEAD request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.HEAD, path, handler);
    }

    /**
     * Adds a TRACE request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin trace(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.TRACE, path, handler);
    }

    /**
     * Adds a CONNECT request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin connect(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.CONNECT, path, handler);
    }

    /**
     * Adds a OPTIONS request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.OPTIONS, path, handler);
    }

    // Secured HTTP verbs
    /**
     * Adds a GET request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.GET, path, handler, permittedRoles);
    }

    /**
     * Adds a POST request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.POST, path, handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.PUT, path, handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.PATCH, path, handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.DELETE, path, handler, permittedRoles);
    }

    /**
     * Adds a HEAD request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.HEAD, path, handler, permittedRoles);
    }

    /**
     * Adds a TRACE request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin trace(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.TRACE, path, handler, permittedRoles);
    }

    /**
     * Adds a CONNECT request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin connect(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.CONNECT, path, handler, permittedRoles);
    }

    /**
     * Adds a CONNECT request handler for the given path to the instance.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.OPTIONS, path, handler, permittedRoles);
    }

    // Filters

    /**
     * Adds a before request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin before(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.BEFORE, path, handler);
    }

    /**
     * Adds a before request handler for all routes in the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin before(@NotNull Handler handler) {
        return before("*", handler);
    }

    /**
     * Adds an after request handler for the given path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin after(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.AFTER, path, handler);
    }

    /**
     * Adds an after request handler for all routes in the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public Javalin after(@NotNull Handler handler) {
        return after("*", handler);
    }

    // Reverse routing

    /**
     * Finds the path for the given handler.
     */
    public String pathFinder(@NotNull Handler handler) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler));
    }

    /**
     * Finds the path for the given handler with the given {@link HandlerType}.
     *
     * @see HandlerType
     */
    public String pathFinder(@NotNull Handler handler, @NotNull HandlerType handlerType) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler) && he.getType() == handlerType);
    }

    // WebSockets
    // Only available via Jetty, as there is no WebSocket interface in Java to build on top of

    private Map<String, Object> pathWsHandlers = new HashMap<>();

    /**
     * Adds a lambda handler for web socket connection requests for the given path.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        WebSocketHandler configuredHandler = new WebSocketHandler();
        ws.configure(configuredHandler);
        return addWebSocketHandler(path, configuredHandler);
    }

    /**
     * Adds a Jetty annotated class as a handler for web socket connection requests for the given path.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Class webSocketClass) {
        return addWebSocketHandler(path, webSocketClass);
    }

    /**
     * Adds a Jetty websocket object as a handler for web socket connection requests for the given path.
     *
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Object webSocketObject) {
        return addWebSocketHandler(path, webSocketObject);
    }

    private Javalin addWebSocketHandler(@NotNull String path, @NotNull Object webSocketObject) {
        ensureActionIsPerformedBeforeServerStart("Configuring WebSockets");
        pathWsHandlers.put(path, webSocketObject);
        return this;
    }

    // package private method used for testing
    EmbeddedServer embeddedServer() {
        return embeddedServer;
    }

    // package private method used for testing
    void clearMatcherAndMappers() {
        pathMatcher.getHandlerEntries().clear();
        errorMapper.getErrorHandlerMap().clear();
        exceptionMapper.getExceptionMap().clear();
    }

}
