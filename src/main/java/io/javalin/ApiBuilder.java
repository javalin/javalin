/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.embeddedserver.jetty.websocket.WebSocketConfig;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

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

    /**
     * Prefixes all handlers defined inside the endpoint group with the given path.
     * All paths are assumed to be relative, so the leading slash will be omitted.
     *
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     */
    public static void path(@NotNull String path, @NotNull EndpointGroup endpointGroup) {
        path = path.startsWith("/") ? path : "/" + path;
        pathDeque.addLast(path);
        endpointGroup.addEndpoints();
        pathDeque.removeLast();
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
     * Adds a GET request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull String path, @NotNull Handler handler) {
        staticInstance().get(prefixPath(path), handler);
    }

    /**
     * Adds a POST request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler) {
        staticInstance().post(prefixPath(path), handler);
    }

    /**
     * Adds a PUT request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler) {
        staticInstance().put(prefixPath(path), handler);
    }

    /**
     * Adds a PATCH request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler) {
        staticInstance().patch(prefixPath(path), handler);
    }

    /**
     * Adds a DELETE request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler) {
        staticInstance().delete(prefixPath(path), handler);
    }

    // Filters

    /**
     * Adds a before request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void before(@NotNull String path, @NotNull Handler handler) {
        staticInstance().before(prefixPath(path), handler);
    }

    /**
     * Adds an after request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    public static void after(@NotNull String path, @NotNull Handler handler) {
        staticInstance().after(prefixPath(path), handler);
    }

    /**
     * Adds a GET request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    // Secured HTTP verbs
    public static void get(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().get(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a POST request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().post(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().put(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(path), handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler for the given path to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull String path, @NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(path), handler, permittedRoles);
    }

    // HTTP verbs (no path specified)
    /**
     * Adds a GET request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void get(@NotNull Handler handler) {
        staticInstance().get(prefixPath(""), handler);
    }

    /**
     * Adds a POST request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void post(@NotNull Handler handler) {
        staticInstance().post(prefixPath(""), handler);
    }

    /**
     * Adds a PUT request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void put(@NotNull Handler handler) {
        staticInstance().put(prefixPath(""), handler);
    }

    /**
     * Adds a PATCH request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void patch(@NotNull Handler handler) {
        staticInstance().patch(prefixPath(""), handler);
    }

    /**
     * Adds a DELETE request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void delete(@NotNull Handler handler) {
        staticInstance().delete(prefixPath(""), handler);
    }

    // Filters

    /**
     * Adds a before request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void before(@NotNull Handler handler) {
        staticInstance().before(prefixPath("/*"), handler);
    }

    /**
     * Adds an after request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     * @see ApiBuilder#path(String, EndpointGroup)
     */
    public static void after(@NotNull Handler handler) {
        staticInstance().after(prefixPath("/*"), handler);
    }

    // Secured HTTP verbs (no path specified)

    /**
     * Adds a GET request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see ApiBuilder#path(String, EndpointGroup)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void get(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().get(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a POST request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see ApiBuilder#path(String, EndpointGroup)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void post(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().post(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a PUT request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see ApiBuilder#path(String, EndpointGroup)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void put(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().put(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a PATCH request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see ApiBuilder#path(String, EndpointGroup)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void patch(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().patch(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a DELETE request handler to the {@link Javalin} instance.
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only.
     * The list of permitted roles will be handled to access manager on request.
     *
     * Requires defined access manager in the instance.
     *
     * @see AccessManager
     * @see Javalin#accessManager(AccessManager)
     * @see ApiBuilder#path(String, EndpointGroup)
     *
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    public static void delete(@NotNull Handler handler, @NotNull List<Role> permittedRoles) {
        staticInstance().delete(prefixPath(""), handler, permittedRoles);
    }

    /**
     * Adds a lambda handler for web socket connection requests for the given path.
     *
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only and before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull WebSocketConfig ws) {
        staticJavalin.ws(prefixPath(path), ws);
    }

    /**
     * Adds a Jetty annotated class as a handler for web socket connection requests for the given path.
     *
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only and before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Class webSocketClass) {
        staticJavalin.ws(prefixPath(path), webSocketClass);
    }

    /**
     * Adds a Jetty websocket object as a handler for web socket connection requests for the given path.
     *
     * The method must be called inside {@link Javalin#routes(EndpointGroup)} only and before {@link Javalin#start()}.
     *
     * @see <a href="https://javalin.io/documentation#websockets">Websockets in docs</a>
     */
    public static void ws(@NotNull String path, @NotNull Object webSocketObject) {
        staticJavalin.ws(prefixPath(path), webSocketObject);
    }

}
