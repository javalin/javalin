package io.javalin.apibuilder

import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.router.Endpoint
import io.javalin.router.JavalinDefaultRoutingApi
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import java.util.ArrayDeque
import java.util.function.Consumer

/**
 * Thread-local state for [ApiBuilder.path] groups: path prefix stack and role scope stack.
 */
internal object ApiBuilderScope {

    private val pathDeque = ThreadLocal.withInitial { ArrayDeque<String>() }
    private val roleDeque = ThreadLocal.withInitial { ArrayDeque<Set<RouteRole>>() }

    @JvmStatic fun pushPath(path: String) = pathDeque.get().addLast(path)
    @JvmStatic fun popPath(): String = pathDeque.get().removeLast()
    @JvmStatic fun prefixPath(path: String): String {
        val normalized = if (path == "*") path
            else if (path.startsWith("/") || path.isEmpty()) path
            else "/$path"
        return pathDeque.get().joinToString("") + normalized
    }

    @JvmStatic fun pushRoles(roles: Collection<RouteRole>) = roleDeque.get().addLast(roles.toSet())
    @JvmStatic fun popRoles() = roleDeque.get().removeLast()
    @JvmStatic fun hasRoles(): Boolean = roleDeque.get().any { it.isNotEmpty() }

    fun scopedRoles(): Set<RouteRole> =
        roleDeque.get().flatMapTo(linkedSetOf()) { it }
}

/**
 * A decorator around [JavalinDefaultRoutingApi] that merges the current [ApiBuilderScope]
 * roles into every endpoint and WebSocket handler registered through it.
 */
internal class ScopedRoutingApi(
    private val delegate: JavalinDefaultRoutingApi,
) : JavalinDefaultRoutingApi {

    override fun addEndpoint(endpoint: Endpoint) = apply {
        val existing = endpoint.metadata(Roles::class.java)?.roles.orEmpty()
        val merged = ApiBuilderScope.scopedRoles() + existing
        delegate.addEndpoint(endpoint.withMetadata(Roles(merged)))
    }

    override fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole) = apply {
        val merged = ApiBuilderScope.scopedRoles() + roles
        delegate.addWsHandler(handlerType, path, wsConfig, *merged.toTypedArray())
    }

    override fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>) = apply {
        delegate.exception(exceptionClass, exceptionHandler)
    }

    override fun error(status: Int, contentType: String, handler: Handler) = apply {
        delegate.error(status, contentType, handler)
    }

    override fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>) = apply {
        delegate.wsException(exceptionClass, exceptionHandler)
    }
}
