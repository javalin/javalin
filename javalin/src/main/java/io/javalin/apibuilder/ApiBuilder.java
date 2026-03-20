/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder;

import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.sse.SseClient;
import io.javalin.router.JavalinDefaultRoutingApi;
import io.javalin.security.RouteRole;
import io.javalin.websocket.WsConfig;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * Static methods for route declarations in Javalin
 */
public class ApiBuilder {

    private static final ThreadLocal<JavalinDefaultRoutingApi> staticJavalin = new ThreadLocal<>();
    private static final ThreadLocal<Deque<String>> pathDeque = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Deque<RouteRole[]>> routeRoleDeque = ThreadLocal.withInitial(ArrayDeque::new);

    /**
     * Sets the static Javalin instance (ThreadLocal) used by the ApiBuilder methods.
     * This is used internally by {@code config.routes.apiBuilder(EndpointGroup)} to enable
     * the static route methods like {@link #get(String, Handler)}, and is exposed publicly
     * to allow creation of custom static route methods.
     *
     * @param javalin the Javalin routing API to use for route registration
     */
    public static void setStaticJavalin(@NotNull JavalinDefaultRoutingApi javalin) {
        staticJavalin.set(javalin);
    }

    /**
     * Clears the static Javalin instance (ThreadLocal) from the current thread.
     *
     * @see #setStaticJavalin(JavalinDefaultRoutingApi)
     */
    public static void clearStaticJavalin() {
        staticJavalin.remove();
    }

    /**
     * Prefixes all handlers defined in its scope with the specified path.
     * All paths are normalized, so you can call both
     * path("/path") or path("path") depending on your preference
     */
    public static void path(@NotNull String path, @NotNull EndpointGroup endpointGroup) {
        path(path, Collections.emptyList(), endpointGroup);
    }

    /**
     * Prefixes all handlers defined in its scope with the specified path and applies
     * the given roles to all endpoints in the group by default.
     * All paths are normalized, so you can call both
     * path("/path") or path("path") depending on your preference
     */
    public static void path(@NotNull String path, @NotNull Collection<RouteRole> roles, @NotNull EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.get().addLast(path);
        routeRoleDeque.get().addLast(roles.toArray(RouteRole[]::new));
        try {
            endpointGroup.addEndpoints();
        } finally {
            pathDeque.get().removeLast();
            routeRoleDeque.get().removeLast();
        }
    }

    public static String prefixPath(@NotNull String path) {
        if (!path.equals("*")) {
            path = (path.startsWith("/") || path.isEmpty()) ? path : "/" + path;
        }
        return String.join("", pathDeque.get()) + path;
    }

    public static JavalinDefaultRoutingApi staticInstance() {
        JavalinDefaultRoutingApi javalin = staticJavalin.get();
        if (javalin == null) {
            throw new IllegalStateException("The static API can only be used within a routes() call.");
        }
        return javalin;
    }

    // ********************************************************************************************
    // HTTP verbs
    // ********************************************************************************************

    /**
     * Adds a GET request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler) {
        staticInstance().get(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().get(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a GET request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler) {
        staticInstance().get(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a GET request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().get(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a POST request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler) {
        staticInstance().post(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().post(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a POST request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler) {
        staticInstance().post(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a QUERY request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@code config.routes.apiBuilder(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void query(@NotNull String path, @NotNull Handler handler) {
        staticInstance().query(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a QUERY request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@code config.routes.apiBuilder(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void query(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().query(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a QUERY request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@code config.routes.apiBuilder(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void query(@NotNull Handler handler) {
        staticInstance().query(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a QUERY request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@code config.routes.apiBuilder(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void query(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().query(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a POST request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().post(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a PUT request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler) {
        staticInstance().put(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().put(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a PUT request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler) {
        staticInstance().put(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a PUT request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().put(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a PATCH request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler) {
        staticInstance().patch(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().patch(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a PATCH request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler) {
        staticInstance().patch(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a PATCH request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().patch(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a DELETE request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler) {
        staticInstance().delete(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().delete(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a DELETE request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler) {
        staticInstance().delete(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a DELETE request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().delete(prefixPath(""), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a HEAD request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull String path, @NotNull Handler handler) {
        staticInstance().head(prefixPath(path), handler, routeRolesInScope());
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().head(prefixPath(path), handler, routeRolesInScope(roles));
    }

    /**
     * Adds a HEAD request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull Handler handler) {
        staticInstance().head(prefixPath(""), handler, routeRolesInScope());
    }

    /**
     * Adds a HEAD request handler with the given roles for the current path to the instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().head(prefixPath(""), handler, routeRolesInScope(roles));
    }

    // ********************************************************************************************
    // Before/after handlers (filters)
    // ********************************************************************************************

    /**
     * Adds a BEFORE request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void before(@NotNull String path, @NotNull Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    /**
     * Adds a BEFORE request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void before(@NotNull Handler handler) {
        staticInstance().before(prefixPath("*"), handler);
    }

    /**
     * Adds a BEFORE_MATCHED request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void beforeMatched(@NotNull String path, @NotNull Handler handler) {
        staticInstance().beforeMatched(prefixPath(path), handler);
    }

    /**
     * Adds a BEFORE_MATCHED request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void beforeMatched(@NotNull Handler handler) {
        staticInstance().beforeMatched(prefixPath("*"), handler);
    }

    /**
     * Adds an AFTER request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void after(@NotNull String path, @NotNull Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    /**
     * Adds a AFTER request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void after(@NotNull Handler handler) {
        staticInstance().after(prefixPath("*"), handler);
    }

    /**
     * Adds a AFTER_MATCHED request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void afterMatched(@NotNull String path, @NotNull Handler handler) {
        staticInstance().afterMatched(prefixPath(path), handler);
    }

    /**
     * Adds a AFTER_MATCHED request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void afterMatched(@NotNull Handler handler) {
        staticInstance().afterMatched(prefixPath("*"), handler);
    }

    // ********************************************************************************************
    // WebSocket
    // ********************************************************************************************

    /**
     * Adds a WebSocket handler on the specified path.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Consumer<WsConfig> ws) {
        staticInstance().ws(prefixPath(path), ws, routeRolesInScope());
    }

    /**
     * Adds a WebSocket handler with the given roles for the specified path.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Consumer<WsConfig> ws, @NotNull RouteRole... roles) {
        staticInstance().ws(prefixPath(path), ws, routeRolesInScope(roles));
    }

    /**
     * Adds a WebSocket handler on the current path.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsConfig> ws) {
        staticInstance().ws(prefixPath(""), ws, routeRolesInScope());
    }

    /**
     * Adds a WebSocket handler with the given roles for the current path.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsConfig> ws, @NotNull RouteRole... roles) {
        staticInstance().ws(prefixPath(""), ws, routeRolesInScope(roles));
    }

    /**
     * Adds a WebSocket before handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     */
    public static void wsBefore(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        staticInstance().wsBefore(prefixPath(path), wsConfig);
    }

    /**
     * Adds a WebSocket before handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     */
    public static void wsBefore(@NotNull Consumer<WsConfig> wsConfig) {
        staticInstance().wsBefore(prefixPath("*"), wsConfig);
    }

    /**
     * Adds a WebSocket after handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     */
    public static void wsAfter(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        staticInstance().wsAfter(prefixPath(path), wsConfig);
    }

    /**
     * Adds a WebSocket after handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     */
    public static void wsAfter(@NotNull Consumer<WsConfig> wsConfig) {
        staticInstance().wsAfter(prefixPath("*"), wsConfig);
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(path), client, routeRolesInScope());
    }

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull RouteRole... roles) {
        staticInstance().sse(prefixPath(path), client, routeRolesInScope(roles));
    }

    public static void sse(@NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(""), client, routeRolesInScope());
    }

    public static void sse(@NotNull Consumer<SseClient> client, @NotNull RouteRole... roles) {
        staticInstance().sse(prefixPath(""), client, routeRolesInScope(roles));
    }

    // ********************************************************************************************
    // CrudHandler
    // ********************************************************************************************

    /**
     * Adds a CrudHandler handler to the current path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void crud(@NotNull CrudHandler crudHandler) {
        crud("", crudHandler, new RouteRole[0]);
    }

    /**
     * Adds a CrudHandler handler to the current path with the given roles to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void crud(@NotNull CrudHandler crudHandler, @NotNull RouteRole... roles) {
        crud("", crudHandler, roles);
    }

    /**
     * Adds a CrudHandler handler to the specified path to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see CrudHandler
     */
    public static void crud(@NotNull String path, @NotNull CrudHandler crudHandler) {
        crud(path, crudHandler, new RouteRole[0]);
    }

    /**
     * Adds a CrudHandler handler to the specified path with the given roles to the {@link Javalin} instance.
     * The method can only be called inside a config.routes.apiBuilder(EndpointGroup).
     *
     * @see CrudHandler
     */
    public static void crud(@NotNull String path, @NotNull CrudHandler crudHandler, @NotNull RouteRole... roles) {
        String fullPath = prefixPath(path);
        String[] subPaths = Arrays.stream(fullPath.split("/")).filter(it -> !it.isEmpty()).toArray(String[]::new);
        if (subPaths.length < 2) {
            throw new IllegalArgumentException("CrudHandler requires a path like '/resource/{resource-id}'");
        }
        String resourceId = subPaths[subPaths.length - 1];
        if (!(resourceId.startsWith("{") && resourceId.endsWith("}"))) {
            throw new IllegalArgumentException("CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/{user-id}'");
        }
        String resourceBase = subPaths[subPaths.length - 2];
        if (resourceBase.startsWith("{") || resourceBase.startsWith("<") || resourceBase.endsWith("}") || resourceBase.endsWith(">")) {
            throw new IllegalArgumentException("CrudHandler requires a resource base at the beginning of the provided path, e.g. '/users/{user-id}'");
        }
        staticInstance().get(fullPath, ctx -> crudHandler.getOne(ctx, ctx.pathParam(resourceId)), routeRolesInScope(roles));
        staticInstance().get(fullPath.replace(resourceId, ""), crudHandler::getAll, routeRolesInScope(roles));
        staticInstance().post(fullPath.replace(resourceId, ""), crudHandler::create, routeRolesInScope(roles));
        staticInstance().patch(fullPath, ctx -> crudHandler.update(ctx, ctx.pathParam(resourceId)), routeRolesInScope(roles));
        staticInstance().delete(fullPath, ctx -> crudHandler.delete(ctx, ctx.pathParam(resourceId)), routeRolesInScope(roles));
    }

    private static RouteRole[] routeRolesInScope(@NotNull RouteRole... roles) {
        if (routeRoleDeque.get().isEmpty()) {
            return roles;
        }

        int scopeRoleCount = roles.length;
        for (RouteRole[] scopeRoles : routeRoleDeque.get()) {
            scopeRoleCount += scopeRoles.length;
        }

        RouteRole[] routeRoles = new RouteRole[scopeRoleCount];
        int routeRoleCount = 0;
        for (RouteRole[] scopeRoles : routeRoleDeque.get()) {
            for (RouteRole role : scopeRoles) {
                routeRoles[routeRoleCount++] = role;
            }
        }
        for (RouteRole role : roles) {
            routeRoles[routeRoleCount++] = role;
        }
        return routeRoles;
    }
}

