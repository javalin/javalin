/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.core.ErrorMapper;
import io.javalin.core.ExceptionMapper;
import io.javalin.core.HandlerType;
import io.javalin.core.PathMatcher;
import io.javalin.core.util.Util;
import io.javalin.embeddedserver.EmbeddedServer;
import io.javalin.embeddedserver.EmbeddedServerFactory;
import io.javalin.embeddedserver.Location;
import io.javalin.embeddedserver.StaticFileConfig;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import io.javalin.event.EventListener;
import io.javalin.event.EventManager;
import io.javalin.event.EventType;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;

public class Javalin {

    private static Logger log = LoggerFactory.getLogger(Javalin.class);

    public static int DEFAULT_PORT = 7000;

    private int port = DEFAULT_PORT;

    private String ipAddress = "0.0.0.0";

    private EmbeddedServer embeddedServer;
    private EmbeddedServerFactory embeddedServerFactory = new EmbeddedJettyFactory();

    private StaticFileConfig staticFileConfig = null;
    PathMatcher pathMatcher = new PathMatcher();
    ExceptionMapper exceptionMapper = new ExceptionMapper();
    ErrorMapper errorMapper = new ErrorMapper();

    private EventManager eventManager = new EventManager();

    private CountDownLatch startLatch = new CountDownLatch(1);
    private CountDownLatch stopLatch = new CountDownLatch(1);

    private AccessManager accessManager = (Handler handler, Context ctx, List<Role> permittedRoles) -> {
        throw new IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'");
    };

    public static Javalin create() {
        return new Javalin();
    }

    // Begin embedded server methods

    private boolean started = false;

    public synchronized Javalin start() {
        if (!started) {
            log.info(Util.INSTANCE.javalinBanner());
            Util.INSTANCE.printHelpfulMessageIfLoggerIsMissing();
            new Thread(() -> {
                eventManager.fireEvent(EventType.SERVER_STARTING, this);
                try {
                    embeddedServer = embeddedServerFactory.create(pathMatcher, exceptionMapper, errorMapper, staticFileConfig);
                    log.info("Starting Javalin ...");
                    port = embeddedServer.start(ipAddress, port);
                    log.info("Javalin has started \\o/");
                } catch (Exception e) {
                    log.error("Failed to start Javalin", e);
                    eventManager.fireEvent(EventType.SERVER_START_FAILED, this);
                }
                eventManager.fireEvent(EventType.SERVER_STARTED, this);
                try {
                    startLatch.countDown();
                    embeddedServer.join();
                } catch (InterruptedException e) {
                    log.error("Server startup interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }).start();
            started = true;
        }
        return this;
    }

    public synchronized Javalin awaitInitialization() {
        if (!started) {
            throw new IllegalStateException("Server hasn't been started. Call start() before calling this method.");
        }
        try {
            startLatch.await();
        } catch (InterruptedException e) {
            log.info("awaitInitialization was interrupted");
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public synchronized Javalin stop() {
        eventManager.fireEvent(EventType.SERVER_STOPPING, this);
        new Thread(() -> {
            log.info("Stopping Javalin ...");
            try {
                embeddedServer.stop();
            } catch (Exception e) {
                log.error("Javalin failed to stop gracefully", e);
            }
            log.info("Javalin has stopped");
            eventManager.fireEvent(EventType.SERVER_STOPPED, this);
            stopLatch.countDown();
        }).start();
        return this;
    }

    public synchronized Javalin awaitTermination() {
        if (!started) {
            throw new IllegalStateException("Server hasn't been stopped. Call stop() before calling this method.");
        }
        try {
            stopLatch.await();
        } catch (InterruptedException e) {
            log.info("awaitTermination was interrupted");
            Thread.currentThread().interrupt();
        }
        return this;
    }

    public synchronized Javalin embeddedServer(EmbeddedServerFactory embeddedServerFactory) {
        ensureServerHasNotStarted();
        this.embeddedServerFactory = embeddedServerFactory;
        return this;
    }

    /*testing*/ EmbeddedServer embeddedServer() {
        return embeddedServer;
    }

    public synchronized Javalin enableStaticFiles(String classpathPath) {
        return enableStaticFiles(classpathPath, Location.CLASSPATH);
    }

    public synchronized Javalin enableStaticFiles(String path, Location location) {
        ensureServerHasNotStarted();
        Util.INSTANCE.notNull("Location cannot be null", path);
        staticFileConfig = new StaticFileConfig(path, location);
        return this;
    }

    public synchronized Javalin ipAddress(String ipAddress) {
        ensureServerHasNotStarted();
        this.ipAddress = ipAddress;
        return this;
    }

    public synchronized int port() {
        return started ? port : -1;
    }

    public synchronized Javalin port(int port) {
        ensureServerHasNotStarted();
        this.port = port;
        return this;
    }

    private void ensureServerHasNotStarted() {
        if (started) {
            throw new IllegalStateException("This must be done before starting the server (adding handlers automatically starts the server)");
        }
    }

    // End embedded server methods

    public synchronized Javalin accessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
        return this;
    }

    public synchronized <T extends Exception> Javalin exception(Class<T> exceptionClass, ExceptionHandler<? super T> exceptionHandler) {
        exceptionMapper.put(exceptionClass, (ExceptionHandler<Exception>) exceptionHandler);
        return this;
    }

    public synchronized Javalin event(EventType eventType, EventListener eventListener) {
        ensureServerHasNotStarted();
        eventManager.addEventListener(eventType, eventListener);
        return this;
    }

    public synchronized Javalin error(int statusCode, ErrorHandler errorHandler) {
        errorMapper.put(statusCode, errorHandler);
        return this;
    }

    public synchronized Javalin routes(ApiBuilder.EndpointGroup endpointGroup) {
        ApiBuilder.setStaticJavalin(this);
        endpointGroup.addEndpoints();
        ApiBuilder.clearStaticJavalin();
        return this;
    }

    public synchronized Javalin addHandler(HandlerType httpMethod, String path, Handler handler) {
        start();
        pathMatcher.add(httpMethod, path, handler);
        return this;
    }

    // HTTP verbs
    public Javalin get(String path, Handler handler) {
        return addHandler(HandlerType.GET, path, handler);
    }

    public Javalin post(String path, Handler handler) {
        return addHandler(HandlerType.POST, path, handler);
    }

    public Javalin put(String path, Handler handler) {
        return addHandler(HandlerType.PUT, path, handler);
    }

    public Javalin patch(String path, Handler handler) {
        return addHandler(HandlerType.PATCH, path, handler);
    }

    public Javalin delete(String path, Handler handler) {
        return addHandler(HandlerType.DELETE, path, handler);
    }

    public Javalin head(String path, Handler handler) {
        return addHandler(HandlerType.HEAD, path, handler);
    }

    public Javalin trace(String path, Handler handler) {
        return addHandler(HandlerType.TRACE, path, handler);
    }

    public Javalin connect(String path, Handler handler) {
        return addHandler(HandlerType.CONNECT, path, handler);
    }

    public Javalin options(String path, Handler handler) {
        return addHandler(HandlerType.OPTIONS, path, handler);
    }

    // Secured HTTP verbs
    public Javalin get(String path, Handler handler, List<Role> permittedRoles) {
        return this.get(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin post(String path, Handler handler, List<Role> permittedRoles) {
        return this.post(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin put(String path, Handler handler, List<Role> permittedRoles) {
        return this.put(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin patch(String path, Handler handler, List<Role> permittedRoles) {
        return this.patch(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin delete(String path, Handler handler, List<Role> permittedRoles) {
        return this.delete(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin head(String path, Handler handler, List<Role> permittedRoles) {
        return this.head(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin trace(String path, Handler handler, List<Role> permittedRoles) {
        return this.trace(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin connect(String path, Handler handler, List<Role> permittedRoles) {
        return this.connect(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    public Javalin options(String path, Handler handler, List<Role> permittedRoles) {
        return this.options(path, ctx -> accessManager.manage(handler, ctx, permittedRoles));
    }

    // Filters
    public Javalin before(String path, Handler handler) {
        return addHandler(HandlerType.BEFORE, path, handler);
    }

    public Javalin before(Handler handler) {
        return before("/*", handler);
    }

    public Javalin after(String path, Handler handler) {
        return addHandler(HandlerType.AFTER, path, handler);
    }

    public Javalin after(Handler handler) {
        return after("/*", handler);
    }

    // Reverse routing
    public String pathFinder(Handler handler) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler));
    }

    public String pathFinder(Handler handler, HandlerType handlerType) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler) && he.getType() == handlerType);
    }

}
