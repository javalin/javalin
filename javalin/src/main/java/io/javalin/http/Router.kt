package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.security.AccessManager
import io.javalin.core.security.RouteRole
import io.javalin.http.sse.SseClient
import io.javalin.websocket.WsConfig
import java.util.function.Consumer

@JvmSuppressWildcards
abstract class Router<T : Router<T>> {

    abstract fun path(path: String): Router<SubRouter>

    @Suppress("UNCHECKED_CAST")
    fun path(path: String, consumer: Consumer<Router<SubRouter>>): T {
        consumer.accept(this.path(path))
        return this as T
    }

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun get(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun get(path: String = "", vararg roles: RouteRole, handler: Handler): T = get(path, handler, roles = roles)

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun get(handler: Handler): T = get("", handler)

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun get(handler: Handler, vararg roles: RouteRole): T = get("", handler, roles = roles)

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun post(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun post(path: String = "", vararg roles: RouteRole, handler: Handler): T = post(path, handler, roles = roles)

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun post(handler: Handler): T = post("", handler)

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun post(handler: Handler, vararg roles: RouteRole): T = post("", handler, roles = roles)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun put(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun put(path: String = "", vararg roles: RouteRole, handler: Handler): T = put(path, handler, roles = roles)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun put(handler: Handler): T = put("", handler)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun put(handler: Handler, vararg roles: RouteRole): T = put("", handler, roles = roles)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun patch(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun patch(path: String = "", vararg roles: RouteRole, handler: Handler): T = patch(path, handler, roles = roles)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun patch(handler: Handler): T = patch("", handler)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun patch(handler: Handler, vararg roles: RouteRole): T = patch("", handler, roles = roles)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun delete(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun delete(path: String = "", vararg roles: RouteRole, handler: Handler): T = delete(path, handler, roles = roles)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun delete(handler: Handler): T = delete("", handler)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun delete(handler: Handler, vararg roles: RouteRole): T = delete("", handler, roles = roles)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun head(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun head(path: String = "", vararg roles: RouteRole, handler: Handler): T = head(path, handler, roles = roles)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun head(handler: Handler): T = head("", handler)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun head(handler: Handler, vararg roles: RouteRole): T = head("", handler, roles = roles)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun options(path: String = "", handler: Handler, vararg roles: RouteRole): T

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    //Helper for kotlin
    fun options(path: String = "", vararg roles: RouteRole, handler: Handler): T = options(path, handler, roles = roles)


    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun options(handler: Handler): T = options("", handler)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun options(handler: Handler, vararg roles: RouteRole): T = options("", handler, roles = roles)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    abstract fun sse(path: String, client: Consumer<SseClient>, vararg roles: RouteRole): T

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    //Helper for kotlin
    fun sse(path: String = "", vararg roles: RouteRole, client: Consumer<SseClient>): T = sse(path, client, roles = roles)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    fun sse(client: Consumer<SseClient>): T = sse("", client)

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun before(path: String = "*", handler: Handler): T

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun before(handler: Handler): T = before("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    abstract fun after(path: String = "*", handler: Handler): T

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    @Suppress("UNCHECKED_CAST")
    fun after(handler: Handler): T = after("*", handler)

    /**
     * Adds a CrudHandler handler to the current path to the [Javalin] instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun crud(crudHandler: CrudHandler): T = crud("", crudHandler)

    /**
     * Adds a CrudHandler handler to the current path to the [Javalin] instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    fun crud(crudHandler: CrudHandler, vararg roles: RouteRole): T = crud("", crudHandler, roles = roles)

    /**
     * Adds a CrudHandler handler to the current path to the [Javalin] instance.
     *
     * @see <a href="https://javalin.io/documentation.handlers">Handlers in docs</a>
     */
    @Suppress("UNCHECKED_CAST")
    fun crud(path: String, crudHandler: CrudHandler, vararg roles: RouteRole): T {
        val subPaths = path.split("/").filter { it.isNotEmpty() }.toList()
        require(subPaths.size >= 2) { "CrudHandler requires a path like '/resource/{resource-id}'" }
        val resourceId = subPaths[subPaths.size - 1]
        require(resourceId.startsWith("{") && resourceId.endsWith("}")) {
            "CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/{user-id}'"
        }
        val resourceBase = subPaths[subPaths.size - 2]
        require(!(resourceBase.startsWith("{") || resourceBase.startsWith("<")
                || resourceBase.endsWith("}") || resourceBase.endsWith(">"))) {
            "CrudHandler requires a resource base at the beginning of the provided path, e.g. '/users/{user-id}'"
        }
        val crudFunctions: Map<CrudFunction, Handler> = crudHandler.getCrudFunctions(resourceId)
        get(path, crudFunctions[CrudFunction.GET_ONE]!!, roles = roles)
        get(path.replace(resourceId, ""), crudFunctions[CrudFunction.GET_ALL]!!, roles = roles)
        post(path.replace(resourceId, ""), crudFunctions[CrudFunction.CREATE]!!, roles = roles)
        patch(path, crudFunctions[CrudFunction.UPDATE]!!, roles = roles)
        delete(path, crudFunctions[CrudFunction.DELETE]!!, roles = roles)
        return this as T
    }

    private fun CrudHandler.getCrudFunctions(resourceId: String): Map<CrudFunction, Handler> = CrudFunction.values()
            .associate { it to CrudFunctionHandler(it, this, resourceId) }

    // WS
    /**
     * Adds a WebSocket handler
     *
     * @see [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(ws: Consumer<WsConfig>): T = ws("", ws)

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    abstract fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): T

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     *
     * @see AccessManager
     *
     * @see [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    //Helper for kotlin
    fun ws(path: String = "", vararg roles: RouteRole, ws: Consumer<WsConfig>): T = ws(path, ws, roles = roles)

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    abstract fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): T

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): T = wsBefore("*", wsConfig)

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    abstract fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): T

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): T = wsAfter("*", wsConfig)
}
