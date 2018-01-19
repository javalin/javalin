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

    private StaticFileConfig staticFileConfig = null;
    private PathMatcher pathMatcher = new PathMatcher();
    private ExceptionMapper exceptionMapper = new ExceptionMapper();
    private ErrorMapper errorMapper = new ErrorMapper();
    private LogLevel logLevel = LogLevel.OFF;

    private EventManager eventManager = new EventManager();

    private AccessManager accessManager = (Handler handler, Context ctx, List<Role> permittedRoles) -> {
        throw new IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'");
    };

    private Javalin() {
    }

    public static Javalin create() {
        Util.INSTANCE.printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond();
        return new Javalin();
    }

    public static Javalin start(int port) {
        return new Javalin()
            .port(port)
            .start();
    }

    // Begin embedded server methods

    private boolean started = false;

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
                    dynamicGzipEnabled
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

    public Javalin dontIgnoreTrailingSlashes() {
        ensureActionIsPerformedBeforeServerStart("Telling Javalin to not ignore slashes");
        pathMatcher.setIgnoreTrailingSlashes(false);
        return this;
    }

    public Javalin embeddedServer(@NotNull EmbeddedServerFactory embeddedServerFactory) {
        ensureActionIsPerformedBeforeServerStart("Setting a custom server");
        this.embeddedServerFactory = embeddedServerFactory;
        return this;
    }

    public Javalin enableStaticFiles(@NotNull String classpathPath) {
        return enableStaticFiles(classpathPath, Location.CLASSPATH);
    }

    public Javalin enableStaticFiles(@NotNull String path, @NotNull Location location) {
        ensureActionIsPerformedBeforeServerStart("Enabling static files");
        staticFileConfig = new StaticFileConfig(path, location);
        return this;
    }

    public String contextPath() {
        return this.contextPath;
    }

    public Javalin contextPath(@NotNull String contextPath) {
        ensureActionIsPerformedBeforeServerStart("Setting the context path");
        this.contextPath = Util.INSTANCE.normalizeContextPath(contextPath);
        return this;
    }

    public int port() {
        return port;
    }

    public Javalin port(int port) {
        ensureActionIsPerformedBeforeServerStart("Setting the port");
        this.port = port;
        return this;
    }

    public Javalin enableStandardRequestLogging() {
        return requestLogLevel(LogLevel.STANDARD);
    }

    public Javalin requestLogLevel(@NotNull LogLevel logLevel) {
        ensureActionIsPerformedBeforeServerStart("Enabling request-logging");
        this.logLevel = logLevel;
        return this;
    }

    public Javalin enableCorsForOrigin(@NotNull String... origin) {
        ensureActionIsPerformedBeforeServerStart("Enabling CORS");
        return CorsUtil.INSTANCE.enableCors(this, origin);
    }

    public Javalin enableCorsForAllOrigins() {
        return enableCorsForOrigin("*");
    }

    public Javalin enableDynamicGzip() {
        ensureActionIsPerformedBeforeServerStart("Enabling dynamic GZIP");
        this.dynamicGzipEnabled = true;
        return this;
    }

    private void ensureActionIsPerformedBeforeServerStart(@NotNull String action) {
        if (started) {
            throw new IllegalStateException(action + " must be done before starting the server");
        }
    }

    // End embedded server methods

    public Javalin accessManager(@NotNull AccessManager accessManager) {
        this.accessManager = accessManager;
        return this;
    }

    public <T extends Exception> Javalin exception(@NotNull Class<T> exceptionClass, @NotNull ExceptionHandler<? super T> exceptionHandler) {
        exceptionMapper.getExceptionMap().put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    public Javalin event(@NotNull EventType eventType, @NotNull EventListener eventListener) {
        ensureActionIsPerformedBeforeServerStart("Event-mapping");
        eventManager.getListenerMap().get(eventType).add(eventListener);
        return this;
    }

    public Javalin error(int statusCode, @NotNull ErrorHandler errorHandler) {
        errorMapper.getErrorHandlerMap().put(statusCode, errorHandler);
        return this;
    }

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
    public Javalin get(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.GET, path, handler);
    }

    public Javalin post(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.POST, path, handler);
    }

    public Javalin put(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PUT, path, handler);
    }

    public Javalin patch(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.PATCH, path, handler);
    }

    public Javalin delete(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.DELETE, path, handler);
    }

    public Javalin head(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.HEAD, path, handler);
    }

    public Javalin trace(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.TRACE, path, handler);
    }

    public Javalin connect(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.CONNECT, path, handler);
    }

    public Javalin options(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.OPTIONS, path, handler);
    }

    // Secured HTTP verbs
    public Javalin get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.GET, path, handler, permittedRoles);
    }

    public Javalin post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.POST, path, handler, permittedRoles);
    }

    public Javalin put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.PUT, path, handler, permittedRoles);
    }

    public Javalin patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.PATCH, path, handler, permittedRoles);
    }

    public Javalin delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.DELETE, path, handler, permittedRoles);
    }

    public Javalin head(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.HEAD, path, handler, permittedRoles);
    }

    public Javalin trace(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.TRACE, path, handler, permittedRoles);
    }

    public Javalin connect(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.CONNECT, path, handler, permittedRoles);
    }

    public Javalin options(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        return addSecuredHandler(HandlerType.OPTIONS, path, handler, permittedRoles);
    }

    // Filters
    public Javalin before(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.BEFORE, path, handler);
    }

    public Javalin before(@NotNull Handler handler) {
        return before("*", handler);
    }

    public Javalin after(@NotNull String path, @NotNull Handler handler) {
        return addHandler(HandlerType.AFTER, path, handler);
    }

    public Javalin after(@NotNull Handler handler) {
        return after("*", handler);
    }

    // Reverse routing
    public String pathFinder(@NotNull Handler handler) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler));
    }

    public String pathFinder(@NotNull Handler handler, @NotNull HandlerType handlerType) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler) && he.getType() == handlerType);
    }

    // WebSockets
    // Only available via Jetty, as there is no WebSocket interface in Java to build on top of

    private Map<String, Object> pathWsHandlers = new HashMap<>();

    public Javalin ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        ensureWebSocketsCallWillWork();
        WebSocketHandler configuredHandler = new WebSocketHandler();
        ws.configure(configuredHandler);
        pathWsHandlers.put(path, configuredHandler);
        return this;
    }

    public Javalin ws(@NotNull String path, @NotNull Class webSocketClass) {
        ensureWebSocketsCallWillWork();
        pathWsHandlers.put(path, webSocketClass);
        return this;
    }

    public Javalin ws(@NotNull String path, @NotNull Object webSocketObject) {
        ensureWebSocketsCallWillWork();
        pathWsHandlers.put(path, webSocketObject);
        return this;
    }

    private void ensureWebSocketsCallWillWork() {
        ensureActionIsPerformedBeforeServerStart("Configuring WebSockets");
        Util.INSTANCE.ensureDependencyPresent("Jetty WebSocket", "org.eclipse.jetty.websocket.api.Session", "org.eclipse.jetty.websocket/websocket-server");
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
