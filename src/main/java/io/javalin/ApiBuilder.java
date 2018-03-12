/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Static methods for routes definitions in Javalin
 *
 * @see Javalin#routes(EndpointGroup)
 */
public class ApiBuilder {

    @FunctionalInterface
    public interface EndpointGroup {
        void addEndpoints();
    }

    static void setStaticJavalin(@NotNull Javalin javalin) {
        staticJavalin = javalin;
    }

    static void clearStaticJavalin() {
        staticJavalin = null;
    }

    private static Javalin staticJavalin;
    private static Deque<String> pathDeque = new ArrayDeque<>();
    private static Deque<Role> roleDeque = new ArrayDeque<>();

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

    /**
     * Wraps an endpoint group using the current AccessManager and adds it to the instance
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void role(@NotNull Role role, @NotNull EndpointGroup endpointGroup) {
        roleDeque.addLast(role);
        endpointGroup.addEndpoints();
        roleDeque.removeLast();
    }

    @Nullable
    private static List<Role> getRoles() {
        return roleDeque.isEmpty() ? null : new ArrayList<>(roleDeque);
    }

    private static String prefixPath(@NotNull String path) {
        return pathDeque.stream().collect(Collectors.joining("")) + path;
    }

    private static Javalin staticInstance() {
        if (staticJavalin == null) {
            throw new IllegalStateException("The static API can only be called within a routes() call");
        }
        return staticJavalin;
    }

    // HTTP verbs

    /**
     * Adds a GET request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler) {
        staticInstance().get(prefixPath(path), handler, getRoles());
    }

    /**
     * Adds a POST request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler) {
        staticInstance().post(prefixPath(path), handler, getRoles());
    }

    /**
     * Adds a PUT request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler) {
        staticInstance().put(prefixPath(path), handler, getRoles());
    }

    /**
     * Adds a PATCH request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler) {
        staticInstance().patch(prefixPath(path), handler, getRoles());
    }

    /**
     * Adds a DELETE request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler) {
        staticInstance().delete(prefixPath(path), handler, getRoles());
    }

    // Filters

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
     * Adds an AFTER request handler for the specified path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void after(@NotNull String path, @NotNull Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    // Secured HTTP verbs

    /**
     * Wraps a GET handler using the current AccessManager and adds it to the instance
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().get(prefixPath(path), handler, roles);
    }

    /**
     * Wraps a POST handler using the current AccessManager and adds it to the instance
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().post(prefixPath(path), handler, roles);
    }

    /**
     * Wraps a PUT handler using the current AccessManager and adds it to the instance
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().put(prefixPath(path), handler, roles);
    }

    /**
     * Wraps a PATCH handler using the current AccessManager and adds it to the instance
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().patch(prefixPath(path), handler, roles);
    }

    /**
     * Wraps a DELETE handler using the current AccessManager and adds it to the instance
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().delete(prefixPath(path), handler, roles);
    }

    // HTTP verbs (no path specified)

    /**
     * Adds a GET request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler) {
        staticInstance().get(prefixPath(""), handler, getRoles());
    }

    /**
     * Adds a POST request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler) {
        staticInstance().post(prefixPath(""), handler, getRoles());
    }

    /**
     * Adds a PUT request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler) {
        staticInstance().put(prefixPath(""), handler, getRoles());
    }

    /**
     * Adds a PATCH request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler) {
        staticInstance().patch(prefixPath(""), handler, getRoles());
    }

    /**
     * Adds a DELETE request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler) {
        staticInstance().delete(prefixPath(""), handler, getRoles());
    }

    // Filters

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
     * Adds a AFTER request handler for the current path to the {@link Javalin} instance.
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void after(@NotNull Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

    /**
     * Wraps a GET handler using the current AccessManager and adds it to
     * the instance using the current path
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().get(prefixPath(""), handler, roles);
    }

    /**
     * Wraps a POST handler using the current AccessManager and adds it to
     * the instance using the current path
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().post(prefixPath(""), handler, roles);
    }

    /**
     * Wraps a PUT handler using the current AccessManager and adds it to
     * the instance using the current path
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().put(prefixPath(""), handler, roles);
    }

    /**
     * Wraps a PATCH handler using the current AccessManager and adds it to
     * the instance using the current path
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().patch(prefixPath(""), handler, roles);
    }

    /**
     * Wraps a DELETE handler using the current AccessManager and adds it to
     * the instance using the current path
     * The method can only be called inside a {@link Javalin#routes(EndpointGroup)}.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        ArrayList<Role> roles = new ArrayList<>(permittedRoles);
        roles.addAll(roleDeque);
        staticInstance().delete(prefixPath(""), handler, roles);
    }

    /**
     * Adds a lambda handler for a WebSocket connection on the specified path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        staticJavalin.ws(prefixPath(path), ws);
    }

    /**
     * Adds a Jetty annotated class as a handler for a WebSocket connection on the specified path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Class webSocketClass) {
        staticJavalin.ws(prefixPath(path), webSocketClass);
    }

    /**
     * Adds a Jetty WebSocket object as a handler for a WebSocket connection on the specified path.
     * The method must be called before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Object webSocketObject) {
        staticJavalin.ws(prefixPath(path), webSocketObject);
    }

}
