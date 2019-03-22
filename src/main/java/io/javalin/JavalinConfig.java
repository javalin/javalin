/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.core.util.JettyServerUtil;
import io.javalin.core.util.SinglePageHandler;
import io.javalin.security.AccessManager;
import io.javalin.security.SecurityUtil;
import io.javalin.staticfiles.Location;
import io.javalin.staticfiles.StaticFileConfig;
import io.javalin.websocket.WsHandler;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;

public class JavalinConfig {

    public Server jettyServer;
    public SessionHandler jettySessionHandler;

    public int port = 7000;
    public long maxRequestCacheBodySize = 4096;
    public boolean autoGenerateEtags = false;
    public boolean caseSensitiveUrls = false;
    public boolean devLogging = false;
    public boolean dynamicGzip = true;
    public boolean ignoreTrailingSlashes = true;
    public boolean prefer405over404 = false;
    public boolean showStartupBanner = true;
    public String contextPath = "/";
    public String defaultContentType = "text/plain";

    public RequestLogger requestLogger = null;
    public WsHandler wsLogger = null;
    public SinglePageHandler singlePageHandler = new SinglePageHandler();
    public Set<StaticFileConfig> staticFileConfig = new HashSet<>();
    public AccessManager accessManager = SecurityUtil::noopAccessManager;
    public Consumer<WebSocketServletFactory> wsFactoryConfig = WebSocketServletFactory::getPolicy;

    public void server(@NotNull Supplier<Server> server) {
        jettyServer = server.get();
    }

    public void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandler) {
        jettySessionHandler = JettyServerUtil.INSTANCE.getValidSessionHandlerOrThrow(sessionHandler);
    }

    public void requestLogger(@NotNull RequestLogger logger) {
        requestLogger = logger;
    }

    public void addStaticFiles(@NotNull String path) {
        addStaticFiles(path, Location.CLASSPATH);
    }

    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        staticFileConfig.add(new StaticFileConfig(path, location));
    }

    public void addSinglePageHandler(@NotNull String path, @NotNull String filePath) {
        addSinglePageHandler(path, filePath, Location.CLASSPATH);
    }

    public void addSinglePageHandler(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        singlePageHandler.add(path, filePath, location);
    }

}
