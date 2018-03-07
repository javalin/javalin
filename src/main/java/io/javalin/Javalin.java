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
import io.javalin.core.util.RouteOverviewEntry;
import io.javalin.core.util.RouteOverviewUtil;
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

    private List<RouteOverviewEntry> routeOverviewEntries = new ArrayList<>();

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
    private long maxRequestCacheBodySize = Long.MAX_VALUE;

    private EventManager eventManager = new EventManager();

    private AccessManager accessManager = (Handler handler, Context ctx, List<Role> permittedRoles) -> {
        throw new IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'");
    };

    private Javalin() {
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
        Util.INSTANCE.printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond();
        return new Javalin();
    }

    /**
     * Creates and starts the application with default parameters on the specified port.
     *
     * @param port to run on
     * @return running application instance.
     * @see Javalin#create()
     * @see Javalin#start()
     */
    public static Javalin start(int port) {
        return new Javalin()
            .port(port)
            .start();
    }

    // Begin embedded server methods

    private boolean started = false;

    /**
     * Synchronously starts the application instance.
     *
     * @return running application instance.
     * @see Javalin#create()
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
                    defaultCharacterEncoding,
                    maxRequestCacheBodySize
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
     * Synchronously stops the application instance.
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
     * Configure instance to treat '/test/' and '/test' as different URLs.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin dontIgnoreTrailingSlashes() {
        ensureActionIsPerformedBeforeServerStart("Telling Javalin to not ignore slashes");
        pathMatcher.setIgnoreTrailingSlashes(false);
        return this;
    }

    /**
     * Configure instance to use a custom embedded server.
     *
     * @see <a href="https://javalin.io/documentation#custom-server">Documentation example</a>
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin embeddedServer(@NotNull EmbeddedServerFactory embeddedServerFactory) {
        ensureActionIsPerformedBeforeServerStart("Setting a custom server");
        this.embeddedServerFactory = embeddedServerFactory;
        return this;
    }

    /**
     * Configure instance to serves static files from path in classpath.
     * The method can be called multiple times for different locations.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#static-files>Static files in docs</a>
     */
    public Javalin enableStaticFiles(@NotNull String classpathPath) {
        return enableStaticFiles(classpathPath, Location.CLASSPATH);
    }

    /**
     * Configure instance to serves static files from path in the given location.
     * The method can be called multiple times for different locations.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#static-files>Static files in docs</a>
     */
    public Javalin enableStaticFiles(@NotNull String path, @NotNull Location location) {
        ensureActionIsPerformedBeforeServerStart("Enabling static files");
        staticFileConfig.add(new StaticFileConfig(path, location));
        return this;
    }

    // Context path (common prefix) for the instance.
    public String contextPath() {
        return this.contextPath;
    }

    /**
     * Configure instance to run on specified context path (common prefix).
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin contextPath(@NotNull String contextPath) {
        ensureActionIsPerformedBeforeServerStart("Setting the context path");
        this.contextPath = Util.INSTANCE.normalizeContextPath(contextPath);
        return this;
    }

    /**
     * Get which port instance is running on
     * Mostly useful if you start the instance with port(0) (random port)
     */
    public int port() {
        return port;
    }

    /**
     * Configure instance to run on specified port.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin port(int port) {
        ensureActionIsPerformedBeforeServerStart("Setting the port");
        this.port = port;
        return this;
    }

    /**
     * Configure instance to use {@link LogLevel#STANDARD} for request logs.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableStandardRequestLogging() {
        return requestLogLevel(LogLevel.STANDARD);
    }

    /**
     * Configure instance use specified log level for request logs.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin requestLogLevel(@NotNull LogLevel logLevel) {
        ensureActionIsPerformedBeforeServerStart("Enabling request-logging");
        this.logLevel = logLevel;
        return this;
    }

    /**
     * Configure instance to accept cross origin requests for specified origins.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableCorsForOrigin(@NotNull String... origin) {
        ensureActionIsPerformedBeforeServerStart("Enabling CORS");
        return CorsUtil.INSTANCE.enableCors(this, origin);
    }

    /**
     * Configure instance to accept cross origin requests for all origins.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableCorsForAllOrigins() {
        return enableCorsForOrigin("*");
    }

    /**
     * Configure instance to use dynamic gzip compression.
     * This will compress all responses larger than 1500 bytes.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableDynamicGzip() {
        ensureActionIsPerformedBeforeServerStart("Enabling dynamic GZIP");
        this.dynamicGzipEnabled = true;
        return this;
    }

    /**
     * Configure instance to display a visual overview of all its mapped routes
     * on the specified path.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin enableRouteOverview(@NotNull String path) {
        ensureActionIsPerformedBeforeServerStart("Enabling route overview");
        RouteOverviewUtil.enableRouteOverview(path, this);
        return this;
    }

    /**
     * Configure instance to use the specified content-type as a default
     * value for all responses. This can be overridden in any Handler.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin defaultContentType(@NotNull String contentType) {
        ensureActionIsPerformedBeforeServerStart("Changing default content type");
        this.defaultContentType = contentType;
        return this;
    }

    /**
     * Configure instance to use the specified character-encoding as a default
     * value for all responses. This can be overridden in any Handler.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin defaultCharacterEncoding(@NotNull String characterEncoding) {
        ensureActionIsPerformedBeforeServerStart("Changing default character encoding");
        this.defaultCharacterEncoding = characterEncoding;
        return this;
    }

    /**
     * Configure instance to stop caching requests larger than the specified body size.
     * The default value is Long.MAX_VALUE
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin maxBodySizeForRequestCache(long bodySizeInBytes) {
        ensureActionIsPerformedBeforeServerStart("Changing request cache body size");
        this.maxRequestCacheBodySize = bodySizeInBytes;
        return this;
    }

    /**
     * Configure instance to not cache any requests.
     * If you call this method you will not be able to log request-bodies.
     * The method must be called before {@link Javalin#start()}.
     */
    public Javalin disableRequestCache() {
        return maxBodySizeForRequestCache(0);
    }

    private void ensureActionIsPerformedBeforeServerStart(@NotNull String action) {
        if (started) {
            throw new IllegalStateException(action + " must be done before starting the server");
        }
    }

    // End embedded server methods

    /**
     * Sets the access manager for the instance. Secured endpoints require one to be set.
     *
     * @see <a href="https://javalin.io/documentation#access-manager">Access manager in docs</a>
     * @see AccessManager
     */
    public Javalin accessManager(@NotNull AccessManager accessManager) {
        this.accessManager = accessManager;
        return this;
    }

    /**
     * Adds an exception mapper to the instance.
     * Useful for turning exceptions into standardized errors/messages/pages
     *
     * @see <a href="https://javalin.io/documentation#exception-mapping">Exception mapping in docs</a>
     */
    public <T extends Exception> Javalin exception(@NotNull Class<T> exceptionClass, @NotNull ExceptionHandler<? super T> exceptionHandler) {
        exceptionMapper.getExceptionMap().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    /**
     * Adds a lifecycle event listener.
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
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
     */
    public Javalin error(int statusCode, @NotNull ErrorHandler errorHandler) {
        errorMapper.getErrorHandlerMap().put(statusCode, errorHandler);
        return this;
    }

    /**
     * Creates a temporary static instance in the scope of the endpointGroup.
     * Allows you to call get(handler), post(handler), etc. without without using the instance prefix.
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

    private Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler, List<Role> roles) {
        String prefixedPath = Util.INSTANCE.prefixContextPath(path, contextPath);
        Handler handlerWrap = roles == null ? handler : ctx -> accessManager.manage(handler, ctx, roles);
        pathMatcher.getHandlerEntries().add(new HandlerEntry(httpMethod, prefixedPath, handlerWrap));
        routeOverviewEntries.add(new RouteOverviewEntry(httpMethod, prefixedPath, handler, roles));
        return this;
    }

    private Javalin addHandler(@NotNull HandlerType httpMethod, @NotNull String path, @NotNull Handler handler) {
        return addHandler(httpMethod, path, handler, null); // no roles set for this route (open to everyone)
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
     * Wraps a GET handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.GET, path, handler, permittedRoles);
    }

    /**
     * Wraps a POST handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.POST, path, handler, permittedRoles);
    }

    /**
     * Wraps a PUT handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.PUT, path, handler, permittedRoles);
    }

    /**
     * Wraps a PATCH handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.PATCH, path, handler, permittedRoles);
    }

    /**
     * Wraps a DELETE handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.DELETE, path, handler, permittedRoles);
    }

    /**
     * Wraps a HEAD handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.HEAD, path, handler, permittedRoles);
    }

    /**
     * Wraps a TRACE handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin trace(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.TRACE, path, handler, permittedRoles);
    }

    /**
     * Wraps a CONNECT handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin connect(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.CONNECT, path, handler, permittedRoles);
    }

    /**
     * Wraps a OPTIONS handler using the current AccessManager and adds it to the instance
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addHandler(HandlerType.OPTIONS, path, handler, permittedRoles);
    }

    // Filters

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

    // Reverse routing

    /**
     * Finds the mapped path for the specified handler
     */
    public String pathFinder(@NotNull Handler handler) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler));
    }

    /**
     * Finds the path for the specified handler and handler type (GET, POST, etc)
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
     * Adds a lambda handler for a WebSocket connection on the given path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        WebSocketHandler configuredHandler = new WebSocketHandler();
        ws.configure(configuredHandler);
        return addWebSocketHandler(path, configuredHandler);
    }

    /**
     * Adds a Jetty annotated class as a handler for a WebSocket connection on the given path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Class webSocketClass) {
        return addWebSocketHandler(path, webSocketClass);
    }

    /**
     * Adds a Jetty WebSocket object as a handler for a WebSocket connection on the given path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public Javalin ws(@NotNull String path, @NotNull Object webSocketObject) {
        return addWebSocketHandler(path, webSocketObject);
    }

    private Javalin addWebSocketHandler(@NotNull String path, @NotNull Object webSocketObject) {
        ensureActionIsPerformedBeforeServerStart("Configuring WebSockets");
        pathWsHandlers.put(path, webSocketObject);
        return this;
    }

    /**
     * Gets the list of RouteOverviewEntry-objects used to build
     * the visual route-overview
     */
    public List<RouteOverviewEntry> getRouteOverviewEntries() {
        return this.routeOverviewEntries;
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
