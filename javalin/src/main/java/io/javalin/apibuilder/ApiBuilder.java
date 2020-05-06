/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder;

import io.javalin.Javalin;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.Handler;
import io.javalin.http.sse.SseClient;
import io.javalin.websocket.WsHandler;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Static methods for route declarations in Javalin
 *
 * @see Javalin#routes(EndpointGroup)
 */
public class ApiBuilder {

    private static Javalin staticJavalin;
    private static Deque<String> pathDeque = new ArrayDeque<>();

    public static void setStaticJavalin(@NotNull Javalin javalin) {
        staticJavalin = javalin;
    }

    public static void clearStaticJavalin() {
        staticJavalin = null;
    }

    /**
     * Prefixes all handlers defined in its scope with the specified path.
     * All paths are normalized, so you can call both
     * path("/path") or path("path") depending on your preference
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public static void path(@NotNull String path, @NotNull EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.addLast(path);
        endpointGroup.addEndpoints();
        pathDeque.removeLast();
    }

    public static String prefixPath(@NotNull String path) {
        return String.join("", pathDeque) + ((path.startsWith("/") || path.isEmpty()) ? path : "/" + path);
    }

    public static Javalin staticInstance() {
        if (staticJavalin == null) {
            throw new IllegalStateException("The static API can only be used within a routes() call.");
        }
        return staticJavalin;
    }

    // ********************************************************************************************
    // HTTP verbs
    // ********************************************************************************************

    /**
     * Adds a GET request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler) {
        staticInstance().get(prefixPath(path), handler);
    }

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().get(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a GET request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler) {
        staticInstance().get(prefixPath(""), handler);
    }

    /**
     * Adds a GET request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().get(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a POST request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler) {
        staticInstance().post(prefixPath(path), handler);
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().post(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a POST request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler) {
        staticInstance().post(prefixPath(""), handler);
    }

    /**
     * Adds a POST request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().post(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler) {
        staticInstance().put(prefixPath(path), handler);
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().put(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler) {
        staticInstance().put(prefixPath(""), handler);
    }

    /**
     * Adds a PUT request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().put(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler) {
        staticInstance().patch(prefixPath(path), handler);
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().patch(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler) {
        staticInstance().patch(prefixPath(""), handler);
    }

    /**
     * Adds a PATCH request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().patch(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler) {
        staticInstance().delete(prefixPath(path), handler);
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().delete(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler) {
        staticInstance().delete(prefixPath(""), handler);
    }

    /**
     * Adds a DELETE request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().delete(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a HEAD request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull String path, @NotNull Handler handler) {
        staticInstance().head(prefixPath(path), handler);
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().head(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a HEAD request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull Handler handler) {
        staticInstance().head(prefixPath(""), handler);
    }

    /**
     * Adds a HEAD request handler with the given roles for the current path to the instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void head(@NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        staticInstance().head(prefixPath(""), handler, permittedRoles);
    }

    // ********************************************************************************************
    // Before/after handlers (filters)
    // ********************************************************************************************

    /**
     * Adds a BEFORE request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void before(@NotNull String path, @NotNull Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    /**
     * Adds a BEFORE request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void before(@NotNull Handler handler) {
        staticInstance().before(prefixPath("/*"), handler);
    }

    /**
     * Adds an AFTER request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void after(@NotNull String path, @NotNull Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    /**
     * Adds a AFTER request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void after(@NotNull Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

    // ********************************************************************************************
    // WebSocket
    // ********************************************************************************************

    /**
     * Adds a WebSocket handler on the specified path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Consumer<WsHandler> ws) {
        staticInstance().ws(prefixPath(path), ws);
    }

    /**
     * Adds a WebSocket handler with the given roles for the specified path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Consumer<WsHandler> ws, @NotNull Set<Role> permittedRoles) {
        staticInstance().ws(prefixPath(path), ws, permittedRoles);
    }

    /**
     * Adds a WebSocket handler on the current path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsHandler> ws) {
        staticInstance().ws(prefixPath(""), ws);
    }

    /**
     * Adds a WebSocket handler with the given roles for the current path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsHandler> ws, @NotNull Set<Role> permittedRoles) {
        staticInstance().ws(prefixPath(""), ws, permittedRoles);
    }

    /**
     * Adds a WebSocket before handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsBefore(@NotNull String path, @NotNull Consumer<WsHandler> wsHandler) {
        return staticInstance().wsBefore(prefixPath(path), wsHandler);
    }

    /**
     * Adds a WebSocket before handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsBefore(@NotNull Consumer<WsHandler> wsHandler) {
        return staticInstance().wsBefore(prefixPath("/*"), wsHandler);
    }

    /**
     * Adds a WebSocket after handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsAfter(@NotNull String path, @NotNull Consumer<WsHandler> wsHandler) {
        return staticInstance().wsAfter(prefixPath(path), wsHandler);
    }

    /**
     * Adds a WebSocket after handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsAfter(@NotNull Consumer<WsHandler> wsHandler) {
        return staticInstance().wsAfter(prefixPath("/*"), wsHandler);
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(path), client);
    }

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull Set<Role> permittedRoles) {
        staticInstance().sse(prefixPath(path), client, permittedRoles);
    }

    public static void sse(@NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(""), client);
    }

    public static void sse(@NotNull Consumer<SseClient> client, @NotNull Set<Role> permittedRoles) {
        staticInstance().sse(prefixPath(""), client, permittedRoles);
    }

    // ********************************************************************************************
    // CrudHandler
    // ********************************************************************************************

    /**
     * Adds a CrudHandler handler to the specified path to the instance.
     *
     * @see CrudHandler
     */
    public static void crud(@NotNull String path, @NotNull CrudHandler crudHandler) {
        ApiBuilder.crud(path, crudHandler, new HashSet<>());
    }

    /**
     * Adds a CrudHandler handler to the specified path with the given roles to the instance.
     *
     * @see CrudHandler
     */
    public static void crud(@NotNull String path, @NotNull CrudHandler crudHandler, @NotNull Set<Role> permittedRoles) {
        path = path.startsWith("/") ? path : "/" + path;
        if (path.startsWith("/:")) {
            throw new IllegalArgumentException("CrudHandler requires a resource base at the beginning of the provided path e.g. '/users/:user-id'");
        }
        if (!path.contains("/:") || path.lastIndexOf("/") > path.lastIndexOf("/:")) {
            throw new IllegalArgumentException("CrudHandler requires a path-parameter at the end of the provided path e.g. '/users/:user-id'");
        }
        final String SEPARATOR = "/:";
        String resourceBase = path.substring(0, path.lastIndexOf(SEPARATOR));
        String resourceId = path.substring(path.lastIndexOf(SEPARATOR) + SEPARATOR.length());

        Map<CrudFunction, Handler> crudFunctions = CrudHandlerKt.getCrudFunctions(crudHandler, resourceId);

        String resourceIdPath = prefixPath(path);
        String resourceBasePath = prefixPath(resourceBase);

        staticInstance().get(resourceIdPath, crudFunctions.get(CrudFunction.GET_ONE), permittedRoles);
        staticInstance().get(resourceBasePath, crudFunctions.get(CrudFunction.GET_ALL), permittedRoles);
        staticInstance().post(resourceBasePath, crudFunctions.get(CrudFunction.CREATE), permittedRoles);
        staticInstance().patch(resourceIdPath, crudFunctions.get(CrudFunction.UPDATE), permittedRoles);
        staticInstance().delete(resourceIdPath, crudFunctions.get(CrudFunction.DELETE), permittedRoles);
    }
}
