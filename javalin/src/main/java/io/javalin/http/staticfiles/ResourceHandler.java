package io.javalin.http.staticfiles;

import io.javalin.compression.CompressionStrategy;
import io.javalin.http.Context;
import io.javalin.security.RouteRole;

import java.util.Set;

public interface ResourceHandler {
    /**
     * Initialize the handler - called during server startup.
     * @param compressionStrategy the compression strategy to use
     */
    default void init(CompressionStrategy compressionStrategy) {}

    boolean canHandle(Context context);

    boolean handle(Context context);

    boolean addStaticFileConfig(StaticFileConfig config);

    Set<RouteRole> resourceRouteRoles(Context ctx);
}
