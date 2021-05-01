package io.javalin.http.staticfiles;

import io.javalin.core.util.Header;
import java.util.Map;
import java.util.stream.Stream;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.jetbrains.annotations.NotNull;
import static java.util.stream.Collectors.toMap;

public class StaticFileConfig {
    // @formatter:off
    public @NotNull String urlPathPrefix = "/";
    public @NotNull String directory = "/public";
    public @NotNull Location location = Location.CLASSPATH;
    public boolean precompress = false;
    public ContextHandler.AliasCheck aliasCheck = null;
    public Map<String, String> headers = Stream.of("").collect(toMap(x -> Header.CACHE_CONTROL, x -> "max-age=0")); // mapOf(Header.CACHE_CONTROL to "max-age=0")
    // @formatter:on
}
