/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.RequestLogger;
import io.javalin.core.util.CorsUtil;
import io.javalin.core.util.SinglePageHandler;
import io.javalin.security.AccessManager;
import io.javalin.security.SecurityUtil;
import io.javalin.staticfiles.JettyResourceHandler;
import io.javalin.staticfiles.Location;
import io.javalin.staticfiles.ResourceHandler;
import io.javalin.staticfiles.StaticFileConfig;
import org.jetbrains.annotations.NotNull;

// @formatter:off
public class JavalinServletConfig {

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public String defaultContentType = "text/plain";
    public String contextPath = "/";
    public Long requestCacheSize = 4096L;
    RequestLogger requestLogger;
    ResourceHandler resourceHandler;
    AccessManager accessManager = SecurityUtil::noopAccessManager;
    SinglePageHandler singlePageHandler = new SinglePageHandler();
    private JavalinServlet servlet;

    public JavalinServletConfig(JavalinServlet servlet) {
        this.servlet = servlet;
    }

    public void enableWebjars() { addStaticFiles("/webjars", Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        if (resourceHandler == null) resourceHandler = new JettyResourceHandler();
        resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath) { addSinglePageRoot(path, filePath, Location.CLASSPATH); }
    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() { enableCorsForOrigins("*"); }
    public void enableCorsForOrigins(@NotNull String... origins) {
        CorsUtil.enableCorsForOrigin(servlet, origins);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public void requestLogger(@NotNull RequestLogger requestLogger) {
        this.requestLogger = requestLogger;
    }

}
// @formatter:on
