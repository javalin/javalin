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

// @formatter:off
public class JavalinServletConfig {

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public String defaultContentType = "text/plain";
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
    public void addStaticFiles(String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(String path, Location location) {
        if (resourceHandler == null) resourceHandler = new JettyResourceHandler();
        resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(String path, String filePath) { addSinglePageRoot(path, filePath, Location.CLASSPATH); }
    public void addSinglePageRoot(String path, String filePath, Location location) {
        singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() { enableCorsForOrigins("*"); }
    public void enableCorsForOrigins(String... origins) {
        CorsUtil.enableCorsForOrigin(servlet, origins);
    }

    public void accessManager(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public void requestLogger(RequestLogger requestLogger) {
        this.requestLogger = requestLogger;
    }

}
// @formatter:on
