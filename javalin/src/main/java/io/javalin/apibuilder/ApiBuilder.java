/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder;

import io.javalin.Javalin;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.RouteRole;
import io.javalin.http.Handler;
import io.javalin.http.sse.SseClient;
import io.javalin.websocket.WsConfig;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

/**
 * Static methods for route declarations in Javalin
 *
 * @see Javalin#routes(EndpointGroup)
 */
public class ApiBuilder {

    private static final ThreadLocal<Javalin> staticJavalin = new ThreadLocal<>();
    private static final ThreadLocal<Deque<String>> pathDeque = ThreadLocal.withInitial(ArrayDeque::new);

    public static void setStaticJavalin(@NotNull Javalin javalin) {
        staticJavalin.set(javalin);
    }

    public static void clearStaticJavalin() {
        staticJavalin.remove();
    }

    /**
     * Prefixes all handlers defined in its scope with the specified path.
     * All paths are normalized, so you can call both
     * path("/path") or path("path") depending on your preference
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public static void path(@NotNull String path, @NotNull EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.get().addLast(path);
        endpointGroup.addEndpoints();
        pathDeque.get().removeLast();
    }

    public static String prefixPath(@NotNull String path) {
        return String.join("", pathDeque.get()) + ((path.startsWith("/") || path.isEmpty()) ? path : "/" + path);
    }

    public static Javalin staticInstance() {
        Javalin javalin = staticJavalin.get();
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
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().get(prefixPath(path), handler, roles);
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
    public static void get(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().get(prefixPath(""), handler, roles);
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
    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().post(prefixPath(path), handler, roles);
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
    public static void post(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().post(prefixPath(""), handler, roles);
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
    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().put(prefixPath(path), handler, roles);
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
    public static void put(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().put(prefixPath(""), handler, roles);
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
    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().patch(prefixPath(path), handler, roles);
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
    public static void patch(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().patch(prefixPath(""), handler, roles);
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
    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().delete(prefixPath(path), handler, roles);
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
    public static void delete(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().delete(prefixPath(""), handler, roles);
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
    public static void head(@NotNull String path, @NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().head(prefixPath(path), handler, roles);
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
    public static void head(@NotNull Handler handler, @NotNull RouteRole... roles) {
        staticInstance().head(prefixPath(""), handler, roles);
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
    public static void ws(@NotNull String path, @NotNull Consumer<WsConfig> ws) {
        staticInstance().ws(prefixPath(path), ws);
    }

    /**
     * Adds a WebSocket handler with the given roles for the specified path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Consumer<WsConfig> ws, @NotNull RouteRole... roles) {
        staticInstance().ws(prefixPath(path), ws, roles);
    }

    /**
     * Adds a WebSocket handler on the current path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsConfig> ws) {
        staticInstance().ws(prefixPath(""), ws);
    }

    /**
     * Adds a WebSocket handler with the given roles for the current path.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull Consumer<WsConfig> ws, @NotNull RouteRole... roles) {
        staticInstance().ws(prefixPath(""), ws, roles);
    }

    /**
     * Adds a WebSocket before handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsBefore(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        return staticInstance().wsBefore(prefixPath(path), wsConfig);
    }

    /**
     * Adds a WebSocket before handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsBefore(@NotNull Consumer<WsConfig> wsConfig) {
        return staticInstance().wsBefore(prefixPath("/*"), wsConfig);
    }

    /**
     * Adds a WebSocket after handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsAfter(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        return staticInstance().wsAfter(prefixPath(path), wsConfig);
    }

    /**
     * Adds a WebSocket after handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     */
    public Javalin wsAfter(@NotNull Consumer<WsConfig> wsConfig) {
        return staticInstance().wsAfter(prefixPath("/*"), wsConfig);
    }

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(path), client);
    }

    public static void sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull RouteRole... roles) {
        staticInstance().sse(prefixPath(path), client, roles);
    }

    public static void sse(@NotNull Consumer<SseClient> client) {
        staticInstance().sse(prefixPath(""), client);
    }

    public static void sse(@NotNull Consumer<SseClient> client, @NotNull RouteRole... roles) {
        staticInstance().sse(prefixPath(""), client, roles);
    }

    // ********************************************************************************************
    // CrudHandler
    // ********************************************************************************************

    /**
     * Adds a CrudHandler handler to the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void crud(@NotNull CrudHandler crudHandler) {
        crud("", crudHandler, new RouteRole[0]);
    }

    /**
     * Adds a CrudHandler handler to the current path with the given roles to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void crud(@NotNull CrudHandler crudHandler, @NotNull RouteRole... roles) {
        crud("", crudHandler, roles);
    }

    /**
     * Adds a CrudHandler handler to the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see CrudHandler
     */
    public static void crud(@NotNull String path, @NotNull CrudHandler crudHandler) {
        crud(path, crudHandler, new RouteRole[0]);
    }

    /**
     * Adds a CrudHandler handler to the specified path with the given roles to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
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
        Map<CrudFunction, Handler> crudFunctions = CrudHandlerKt.getCrudFunctions(crudHandler, resourceId);
        staticInstance().get(fullPath, crudFunctions.get(CrudFunction.GET_ONE), roles);
        staticInstance().get(fullPath.replace(resourceId, ""), crudFunctions.get(CrudFunction.GET_ALL), roles);
        staticInstance().post(fullPath.replace(resourceId, ""), crudFunctions.get(CrudFunction.CREATE), roles);
        staticInstance().patch(fullPath, crudFunctions.get(CrudFunction.UPDATE), roles);
        staticInstance().delete(fullPath, crudFunctions.get(CrudFunction.DELETE), roles);
    }
}
