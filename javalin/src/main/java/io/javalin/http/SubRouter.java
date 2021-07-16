package io.javalin.http;

import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.http.sse.SseClient;
import io.javalin.http.sse.SseHandler;
import io.javalin.websocket.WsConfig;
import io.javalin.websocket.WsHandlerType;
import java.util.Set;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;

public final class SubRouter extends Router<SubRouter> {

    private final RouterContext routerContext;
    private final String path;

    public SubRouter(RouterContext routerContext, String path) {
        this.routerContext = routerContext;
        this.path = path;
    }

    private String prefixPath(String path) {
        return (path.startsWith("/") || path.isEmpty()) ? path : "/" + path;
    }

    @Override
    @NotNull
    public SubRouter path(@NotNull String path) {
        return new SubRouter(routerContext, this.path + prefixPath(path));
    }

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter get(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.GET, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter post(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.POST, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter put(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.PUT, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter patch(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.PATCH, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter delete(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.DELETE, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter head(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.HEAD, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter options(@NotNull String path, @NotNull Handler handler, @NotNull Set<Role> permittedRoles) {
        routerContext.addHandler(HandlerType.OPTIONS, this.path + prefixPath(path), handler, permittedRoles);
        return this;
    }

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    @NotNull
    public SubRouter sse(@NotNull String path, @NotNull Consumer<SseClient> client, @NotNull Set<Role> permittedRoles) {
        return get(this.path + prefixPath(path), new SseHandler(client), permittedRoles);
    }

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter before(@NotNull String path, @NotNull Handler handler) {
        routerContext.addHandler(HandlerType.BEFORE, this.path + prefixPath(path), handler);
        return this;
    }

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation#before-handlers">Handlers in docs</a>
     */
    @NotNull
    public SubRouter after(@NotNull String path, @NotNull Handler handler) {
        routerContext.addHandler(HandlerType.AFTER, this.path + prefixPath(path), handler);
        return this;
    }

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     * @see <a href="https://javalin.io/documentation#websockets">WebSockets in docs</a>
     */
    @NotNull
    public SubRouter ws(@NotNull String path, @NotNull Consumer<WsConfig> ws, @NotNull Set<Role> permittedRoles) {
        routerContext.addWsHandler(WsHandlerType.WEBSOCKET, this.path + prefixPath(path), ws, permittedRoles);
        return this;
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    @NotNull
    public SubRouter wsBefore(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        routerContext.addWsHandler(WsHandlerType.WS_BEFORE, this.path + prefixPath(path), wsConfig);
        return this;
    }

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    @NotNull
    public SubRouter wsAfter(@NotNull String path, @NotNull Consumer<WsConfig> wsConfig) {
        routerContext.addWsHandler(WsHandlerType.WS_AFTER, this.path + prefixPath(path), wsConfig);
        return this;
    }

}
