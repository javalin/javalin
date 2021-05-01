package io.javalin.http.staticfiles;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.jetbrains.annotations.NotNull;

public class StaticFileConfig {
    // @formatter:off
    public @NotNull String urlPathPrefix = "/";
    public @NotNull String directory = "/public";
    public @NotNull Location location = Location.CLASSPATH;
    public boolean precompress = false;
    public ContextHandler.AliasCheck aliasCheck = null;
    // @formatter:on
}
