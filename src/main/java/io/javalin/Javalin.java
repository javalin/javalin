/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.core.ErrorMapper;
import io.javalin.core.ExceptionMapper;
import io.javalin.core.PathMatcher;
import io.javalin.core.util.Util;
import io.javalin.embeddedserver.EmbeddedServer;
import io.javalin.embeddedserver.EmbeddedServerFactory;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import io.javalin.lifecycle.Event;
import io.javalin.lifecycle.EventListener;
import io.javalin.lifecycle.EventManager;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;

public class Javalin {

    private static Logger log = LoggerFactory.getLogger(Javalin.class);

    public static int DEFAULT_PORT = 7000;

    private int port = DEFAULT_PORT;

    private String ipAddress = "0.0.0.0";

    private EmbeddedServer embeddedServer;
    private EmbeddedServerFactory embeddedServerFactory = new EmbeddedJettyFactory();

    private String staticFileDirectory = null;
    PathMatcher pathMatcher = new PathMatcher();
    ExceptionMapper exceptionMapper = new ExceptionMapper();
    ErrorMapper errorMapper = new ErrorMapper();

    private EventManager eventManager = new EventManager();

    private Consumer<Exception> startupExceptionHandler = (e) -> log.error("Failed to start Javalin", e);

    private CountDownLatch startLatch = new CountDownLatch(1);
    private CountDownLatch stopLatch = new CountDownLatch(1);

    private AccessManager accessManager = (Handler handler, Request request, Response response, List<? extends Role> permittedRoles) -> {
        throw new IllegalStateException("No access manager configured. Add an access manager using 'accessManager()'");
    };

    public Javalin accessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
        return this;
    }

    public static Javalin create() {
        return new Javalin();
    }

    private boolean started = false;

    public synchronized Javalin start() {
        if (!started) {
            log.info("\n" + Util.INSTANCE.javalinBanner());
            Util.INSTANCE.printHelpfulMessageIfLoggerIsMissing();
            new Thread(() -> {
                eventManager.fireEvent(Event.Type.SERVER_STARTING, this);
                try {
                    embeddedServer = embeddedServerFactory.create(pathMatcher, exceptionMapper, errorMapper, staticFileDirectory);
                    port = embeddedServer.start(ipAddress, port);
                } catch (Exception e) {
                    startupExceptionHandler.accept(e);
                }
                eventManager.fireEvent(Event.Type.SERVER_STARTED, this);
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

    public Javalin awaitInitialization() {
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
        eventManager.fireEvent(Event.Type.SERVER_STOPPING, this);
        new Thread(() -> {
            embeddedServer.stop();
            started = false;
            startLatch = new CountDownLatch(1);
            eventManager.fireEvent(Event.Type.SERVER_STOPPED, this);
            pathMatcher = new PathMatcher();
            exceptionMapper = new ExceptionMapper();
            errorMapper = new ErrorMapper();
            stopLatch.countDown();
            stopLatch = new CountDownLatch(1);
        }).start();
        return this;
    }

    public Javalin awaitTermination() {
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

    public Javalin embeddedServer(EmbeddedServerFactory embeddedServerFactory) {
        ensureServerHasNotStarted();
        this.embeddedServerFactory = embeddedServerFactory;
        return this;
    }

    /*testing*/ EmbeddedServer embeddedServer() {
        return embeddedServer;
    }

    public synchronized Javalin enableStaticFiles(String location) {
        ensureServerHasNotStarted();
        Util.INSTANCE.notNull("Location cannot be null", location);
        staticFileDirectory = location;
        return this;
    }

    public synchronized Javalin ipAddress(String ipAddress) {
        ensureServerHasNotStarted();
        this.ipAddress = ipAddress;
        return this;
    }

    public synchronized Javalin port(int port) {
        ensureServerHasNotStarted();
        this.port = port;
        return this;
    }

    public synchronized Javalin event(Event.Type eventType, EventListener eventListener) {
        ensureServerHasNotStarted();
        eventManager.addEventListener(eventType, eventListener);
        return this;
    }

    public Javalin startupExceptionHandler(Consumer<Exception> startupExceptionHandler) {
        ensureServerHasNotStarted();
        this.startupExceptionHandler = startupExceptionHandler;
        return this;
    }

    private void ensureServerHasNotStarted() {
        if (started) {
            throw new IllegalStateException("This must be done before starting the server (adding handlers automatically starts the server)");
        }
    }

    public synchronized int port() {
        return started ? port : -1;
    }

    public synchronized <T extends Exception> Javalin exception(Class<T> exceptionClass, ExceptionHandler exceptionHandler) {
        exceptionMapper.put(exceptionClass, exceptionHandler);
        return this;
    }

    public synchronized Javalin error(int statusCode, ErrorHandler errorHandler) {
        errorMapper.put(statusCode, errorHandler);
        return this;
    }

    public Javalin routes(ApiBuilder.EndpointGroup endpointGroup) {
        ApiBuilder.setStaticJavalin(this);
        endpointGroup.addEndpoints();
        ApiBuilder.clearStaticJavalin();
        return this;
    }

    public Javalin addHandler(Handler.Type httpMethod, String path, Handler handler) {
        start();
        pathMatcher.add(httpMethod, path, handler);
        return this;
    }

    // HTTP verbs
    public Javalin get(String path, Handler handler) {
        return addHandler(Handler.Type.GET, path, handler);
    }

    public Javalin post(String path, Handler handler) {
        return addHandler(Handler.Type.POST, path, handler);
    }

    public Javalin put(String path, Handler handler) {
        return addHandler(Handler.Type.PUT, path, handler);
    }

    public Javalin patch(String path, Handler handler) {
        return addHandler(Handler.Type.PATCH, path, handler);
    }

    public Javalin delete(String path, Handler handler) {
        return addHandler(Handler.Type.DELETE, path, handler);
    }

    public Javalin head(String path, Handler handler) {
        return addHandler(Handler.Type.HEAD, path, handler);
    }

    public Javalin trace(String path, Handler handler) {
        return addHandler(Handler.Type.TRACE, path, handler);
    }

    public Javalin connect(String path, Handler handler) {
        return addHandler(Handler.Type.CONNECT, path, handler);
    }

    public Javalin options(String path, Handler handler) {
        return addHandler(Handler.Type.OPTIONS, path, handler);
    }

    // Secured HTTP verbs
    public Javalin get(String path, Handler handler, List<Role> permittedRoles) {
        return this.get(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin post(String path, Handler handler, List<Role> permittedRoles) {
        return this.post(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin put(String path, Handler handler, List<Role> permittedRoles) {
        return this.put(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin patch(String path, Handler handler, List<Role> permittedRoles) {
        return this.patch(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin delete(String path, Handler handler, List<Role> permittedRoles) {
        return this.delete(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin head(String path, Handler handler, List<Role> permittedRoles) {
        return this.head(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin trace(String path, Handler handler, List<Role> permittedRoles) {
        return this.trace(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin connect(String path, Handler handler, List<Role> permittedRoles) {
        return this.connect(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    public Javalin options(String path, Handler handler, List<Role> permittedRoles) {
        return this.options(path, (req, res) -> accessManager.manage(handler, req, res, permittedRoles));
    }

    // Filters
    public Javalin before(String path, Handler handler) {
        return addHandler(Handler.Type.BEFORE, path, handler);
    }

    public Javalin before(Handler handler) {
        return before("/*", handler);
    }

    public Javalin after(String path, Handler handler) {
        return addHandler(Handler.Type.AFTER, path, handler);
    }

    public Javalin after(Handler handler) {
        return after("/*", handler);
    }

    // Reverse routing
    public String pathFinder(Handler handler) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler));
    }

    public String pathFinder(Handler handler, Handler.Type handlerType) {
        return pathMatcher.findHandlerPath(he -> he.getHandler().equals(handler) && he.getType() == handlerType);
    }

}
