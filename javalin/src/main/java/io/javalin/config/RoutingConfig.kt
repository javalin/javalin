package io.javalin.config

import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.AFTER
import io.javalin.http.HandlerType.BEFORE
import io.javalin.http.HandlerType.DELETE
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.HandlerType.PATCH
import io.javalin.http.HandlerType.POST
import io.javalin.http.HandlerType.PUT
import io.javalin.http.HttpStatus
import io.javalin.http.sse.SseClient
import io.javalin.http.sse.SseHandler
import io.javalin.routing.HandlerEntry
import io.javalin.security.RouteRole
import io.javalin.util.Util.prefixContextPath
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsHandlerType.WEBSOCKET
import io.javalin.websocket.WsHandlerType.WS_AFTER
import io.javalin.websocket.WsHandlerType.WS_BEFORE
import java.util.function.Consumer

class RoutingConfig(internal val pvt: PrivateConfig) {

    @JvmField var contextPath = "/"
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var treatMultipleSlashesAsSingleSlash = false
    @JvmField var caseInsensitiveRoutes = false

    class JavalinRouter(private val routingConfig: RoutingConfig) : DefaultJavalinRouter<JavalinRouter>() {
        override fun routingConfig(): RoutingConfig = routingConfig
    }
    @JvmField var router = JavalinRouter(this)

    fun <R> router(router: Router<R>, routingScope: R.() -> Unit): RoutingConfig =
        router(router, Consumer { routingScope(it) })

    fun <R> router(router: Router<R>, routingScope: Consumer<R>): RoutingConfig = also {
        router.handle(it, routingScope)
    }

}

fun interface Router<R> {
    fun handle(cfg: RoutingConfig, routingScope: Consumer<R>)
}

abstract class DefaultJavalinRouter<T : DefaultJavalinRouter<T>> {

    protected abstract fun routingConfig(): RoutingConfig

    private fun getThis(): T {
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    // ********************************************************************************************
    // HTTP
    // ********************************************************************************************
    /**
     * Adds an exception mapper to the instance.
     *
     * @see [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): T {
        @Suppress("UNCHECKED_CAST")
        routingConfig().pvt.exceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
        return getThis()
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, handler: Handler): T = error(status.code, "*", handler)

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: Int, handler: Handler): T = error(status, "*", handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, contentType: String, handler: Handler): T = error(status.code, contentType, handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     *
     * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: Int, contentType: String, handler: Handler): T {
        routingConfig().pvt.errorMapper.addHandler(status, contentType, handler)
        return getThis()
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * @see io.javalin.security.AccessManager
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): T {
        val roleSet = HashSet(roles.asList())
        routingConfig().pvt.pathMatcher.add(HandlerEntry(handlerType, path, routingConfig(), roleSet, handler))
        routingConfig().pvt.eventManager.fireHandlerAddedEvent(HandlerMetaInfo(handlerType, prefixContextPath(routingConfig().contextPath, path), handler, roleSet))
        return getThis()
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(httpMethod: HandlerType, path: String, handler: Handler): T =
        addHandler(httpMethod, path, handler, *emptyArray()) // no roles set for this route (open to everyone with default access manager)

    /**
     * Adds a GET request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    operator fun get(path: String, handler: Handler): T = addHandler(GET, path, handler)

    /**
     * Adds a POST request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun post(path: String, handler: Handler): T = addHandler(POST, path, handler)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun put(path: String, handler: Handler): T = addHandler(PUT, path, handler)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun patch(path: String, handler: Handler): T = addHandler(PATCH, path, handler)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun delete(path: String, handler: Handler): T = addHandler(DELETE, path, handler)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun head(path: String, handler: Handler): T = addHandler(HEAD, path, handler)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     * @see [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun options(path: String, handler: Handler): T = addHandler(OPTIONS, path, handler)

    // ********************************************************************************************
    // Secured HTTP verbs
    // ********************************************************************************************

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    operator fun get(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(GET, path, handler, *roles)

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun post(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(POST, path, handler, *roles)

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun put(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(PUT, path, handler, *roles)

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun patch(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(PATCH, path, handler, *roles)

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun delete(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(DELETE, path, handler, *roles)

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun head(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(HEAD, path, handler, *roles)

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun options(path: String, handler: Handler, vararg roles: RouteRole): T = addHandler(OPTIONS, path, handler, *roles)

    // ********************************************************************************************
    // Server-sent events
    // ********************************************************************************************

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>): T = sse(path, client, *emptyArray())

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, handler: SseHandler): T = get(path, handler)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    fun sse(path: String, client: Consumer<SseClient>, vararg roles: RouteRole): T = get(path, SseHandler(clientConsumer = client), *roles)

    // ********************************************************************************************
    // Before/after handlers (filters)
    // ********************************************************************************************

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     * See [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(path: String, handler: Handler): T = addHandler(BEFORE, path, handler)

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     * See [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(handler: Handler): T = before("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(path: String, handler: Handler): T = addHandler(AFTER, path, handler)

    /**
     * Adds an AFTER request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(handler: Handler): T = after("*", handler)

    // ********************************************************************************************
    // WebSocket
    // ********************************************************************************************

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): T {
        @Suppress("UNCHECKED_CAST")
        routingConfig().pvt.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
        return getThis()
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    private fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): T {
        val roleSet = HashSet(roles.asList())
        routingConfig().pvt.cfg.jetty.addHandler(handlerType, path, wsConfig, roleSet)
        routingConfig().pvt.eventManager.fireWsHandlerAddedEvent(
            WsHandlerMetaInfo(
                handlerType,
                prefixContextPath(routingConfig().contextPath, path),
                wsConfig,
                roleSet
            )
        )
        return getThis()
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     */
    private fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>): T = addWsHandler(handlerType, path, wsConfig, *emptyArray())

    /**
     * Adds a WebSocket handler on the specified path.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>): T = ws(path, ws, *emptyArray())

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     * @see io.javalin.security.AccessManager
     */
    fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): T = addWsHandler(WEBSOCKET, path, ws, *roles)

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): T = addWsHandler(WS_BEFORE, path, wsConfig)

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): T = wsBefore("*", wsConfig)

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): T = addWsHandler(WS_AFTER, path, wsConfig)

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): T = wsAfter("*", wsConfig)

}
