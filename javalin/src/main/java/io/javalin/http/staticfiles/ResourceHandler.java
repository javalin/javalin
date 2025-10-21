package io.javalin.http.staticfiles;

import io.javalin.http.Context;
import io.javalin.security.RouteRole;

import java.util.Set;

public interface ResourceHandler {
    boolean canHandle(Context context);

    boolean handle(Context context);

    boolean addStaticFileConfig(StaticFileConfig config);

    Set<RouteRole> resourceRouteRoles(Context ctx);
}
