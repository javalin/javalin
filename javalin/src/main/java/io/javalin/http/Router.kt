package io.javalin.http

import io.javalin.apibuilder.CrudFunction
import io.javalin.apibuilder.CrudHandler
import io.javalin.apibuilder.getCrudFunctions
import io.javalin.core.security.Role
import io.javalin.http.sse.SseClient
import java.util.*
import java.util.function.Consumer

@JvmSuppressWildcards
abstract class Router<T : Router<T>> {

    abstract fun path(path: String): Router<SubRouter>
    fun path(path: String, consumer: Consumer<Router<SubRouter>>): T {
        consumer.accept(this.path(path))
        return this as T
    }

    /**
     * Adds a GET request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun get(path: String = "", handler: Handler, permittedRoles: Set<Role> = setOf()): T
    fun get(handler: Handler): T = get("", handler, setOf())
    fun get(handler: Handler, permittedRoles: Set<Role>): T = get("", handler, permittedRoles)
    fun get(path: String, handler: Handler): T = get(path, handler, setOf())

    /**
     * Adds a POST request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun post(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun post(handler: Handler): T = post("", handler, setOf())
    fun post(handler: Handler, permittedRoles: Set<Role>): T = post("", handler, permittedRoles)
    fun post(path: String, handler: Handler): T = post(path, handler, setOf())

    /**
     * Adds a PUT request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun put(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun put(handler: Handler): T = put("", handler, setOf())
    fun put(handler: Handler, permittedRoles: Set<Role>): T = put("", handler, permittedRoles)
    fun put(path: String, handler: Handler): T = put(path, handler, setOf())

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun patch(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun patch(handler: Handler): T = patch("", handler, setOf())
    fun patch(handler: Handler, permittedRoles: Set<Role>): T = patch("", handler, permittedRoles)
    fun patch(path: String, handler: Handler): T = patch(path, handler, setOf())

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun delete(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun delete(handler: Handler): T = delete("", handler, setOf())
    fun delete(handler: Handler, permittedRoles: Set<Role>): T = delete("", handler, permittedRoles)
    fun delete(path: String, handler: Handler): T = delete(path, handler, setOf())

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun head(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun head(handler: Handler): T = head("", handler, setOf())
    fun head(handler: Handler, permittedRoles: Set<Role>): T = head("", handler, permittedRoles)
    fun head(path: String, handler: Handler): T = head(path, handler, setOf())

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    abstract fun options(path: String = "", handler: Handler, permittedRoles: Set<Role> = HashSet()): T
    fun options(handler: Handler): T = options("", handler, setOf())
    fun options(handler: Handler, permittedRoles: Set<Role>): T = options("", handler, permittedRoles)
    fun options(path: String, handler: Handler): T = options(path, handler, setOf())

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    abstract fun sse(path: String, client: Consumer<SseClient>, permittedRoles: Set<Role> = HashSet()): T
    fun sse(client: Consumer<SseClient>): T = sse("", client, setOf())
    fun sse(path: String, client: Consumer<SseClient>): T = sse(path, client, setOf())

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    abstract fun before(path: String = "*", handler: Handler): T
    fun before(handler: Handler): T = before("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     *
     * @see [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    abstract fun after(path: String = "*", handler: Handler): T
    fun after(handler: Handler): T = after("*", handler)

    /**
     * Adds a CrudHandler handler to the current path to the [Javalin] instance.
     * The method can only be called inside a [Javalin.routes].
     *
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */

    fun crud(crudHandler: CrudHandler): T = crud("", crudHandler, setOf())
    fun crud(crudHandler: CrudHandler, permittedRoles: Set<Role>): T = crud("", crudHandler, permittedRoles)
    fun crud(path: String, crudHandler: CrudHandler): T = crud(path, crudHandler, setOf())


    /**
     * Adds a CrudHandler handler to the specified path with the given roles to the [Javalin] instance.
     * The method can only be called inside a [Javalin.routes].
     *
     * @see CrudHandler
     */
    @Suppress("UNCHECKED_CAST")
    fun crud(path: String, crudHandler: CrudHandler, permittedRoles: Set<Role>): T {
        val subPaths = path.split("/").filter { it.isNotEmpty() }.toList()
        require(subPaths.size >= 2) { "CrudHandler requires a path like '/resource/:resource-id'" }
        val resourceId = subPaths[subPaths.size - 1]
        require(resourceId.startsWith(":")) { "CrudHandler requires a path-parameter at the end of the provided path, e.g. '/users/:user-id'" }
        require(!subPaths[subPaths.size - 2].startsWith(":")) { "CrudHandler requires a resource base at the beginning of the provided path, e.g. '/users/:user-id'" }
        val crudFunctions = crudHandler.getCrudFunctions(resourceId)
        get(path, crudFunctions[CrudFunction.GET_ONE]!!, permittedRoles)
        get(path.replace(resourceId, ""), crudFunctions[CrudFunction.GET_ALL]!!, permittedRoles)
        post(path.replace(resourceId, ""), crudFunctions[CrudFunction.CREATE]!!, permittedRoles)
        patch(path, crudFunctions[CrudFunction.UPDATE]!!, permittedRoles)
        delete(path, crudFunctions[CrudFunction.DELETE]!!, permittedRoles)
        return this as T
    }
}
