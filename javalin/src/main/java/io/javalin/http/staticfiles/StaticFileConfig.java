package io.javalin.http.staticfiles;

import io.javalin.core.util.Header;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.jetbrains.annotations.NotNull;
import static java.util.Collections.singletonMap;

public class StaticFileConfig {
    // @formatter:off
    public @NotNull String hostedPath = "/";
    public @NotNull String directory = "/public";
    public @NotNull Location location = Location.CLASSPATH;
    public boolean precompress = false;
    public ContextHandler.AliasCheck aliasCheck = null;
    public Map<String, String> headers = new HashMap<>(singletonMap(Header.CACHE_CONTROL, "max-age=0"));
    // @formatter:on
}
