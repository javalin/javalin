package io.javalin.router

import io.javalin.config.RoutingConfig
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
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType.WEBSOCKET
import io.javalin.websocket.WsHandlerType.WS_AFTER
import io.javalin.websocket.WsHandlerType.WS_BEFORE
import java.util.function.Consumer

class JavalinRouter(internalRouter: InternalRouter) : AbstractJavalinRouter<JavalinRouter, JavalinRouter>(internalRouter) {

    companion object {
        @JvmStatic
        val JavalinRouter: RouterFactory<JavalinRouter, JavalinRouter> = object : RouterFactory<JavalinRouter, JavalinRouter> {
            override fun create(internalRouter: InternalRouter, setup: Consumer<JavalinRouter>): JavalinRouter {
                val javalinRouter = JavalinRouter(internalRouter)
                setup.accept(javalinRouter)
                return javalinRouter
            }
        }
    }

}

abstract class AbstractJavalinRouter<ROUTER : Router<ROUTER, SETUP>, SETUP>(private val internalRouter: InternalRouter) : Router<ROUTER, SETUP> {

    protected open fun routingConfig(): RoutingConfig = internalRouter.routingConfig

    @Suppress("UNCHECKED_CAST")
    private fun getThis(): ROUTER = this as ROUTER

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    override fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): ROUTER {
        internalRouter.exception(exceptionClass, exceptionHandler)
        return getThis()
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, handler: Handler): ROUTER = error(status.code, "*", handler)

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: Int, handler: Handler): ROUTER = error(status, "*", handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, contentType: String, handler: Handler): ROUTER = error(status.code, contentType, handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    override fun error(status: Int, contentType: String, handler: Handler): ROUTER {
        internalRouter.error(status, contentType, handler)
        return getThis()
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    override fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): ROUTER {
        internalRouter.addHandler(handlerType, path, handler, *roles)
        return getThis()
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(httpMethod: HandlerType, path: String, handler: Handler): ROUTER = addHandler(httpMethod, path, handler, *emptyArray())

    /**
     * Adds a GET request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    operator fun get(path: String, handler: Handler): ROUTER = addHandler(GET, path, handler)

    /**
     * Adds a POST request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun post(path: String, handler: Handler): ROUTER = addHandler(POST, path, handler)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun put(path: String, handler: Handler): ROUTER = addHandler(PUT, path, handler)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun patch(path: String, handler: Handler): ROUTER = addHandler(PATCH, path, handler)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun delete(path: String, handler: Handler): ROUTER = addHandler(DELETE, path, handler)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun head(path: String, handler: Handler): ROUTER = addHandler(HEAD, path, handler)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun options(path: String, handler: Handler): ROUTER = addHandler(OPTIONS, path, handler)

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    operator fun get(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(GET, path, handler, *roles)

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun post(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(POST, path, handler, *roles)

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun put(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(PUT, path, handler, *roles)

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun patch(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(PATCH, path, handler, *roles)

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun delete(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(DELETE, path, handler, *roles)

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun head(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(HEAD, path, handler, *roles)

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun options(path: String, handler: Handler, vararg roles: RouteRole): ROUTER = addHandler(OPTIONS, path, handler, *roles)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>): ROUTER = sse(path, client, *emptyArray())

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, handler: SseHandler): ROUTER = get(path, handler)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    fun sse(path: String, client: Consumer<SseClient>, vararg roles: RouteRole): ROUTER = get(path, SseHandler(clientConsumer = client), *roles)

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(path: String, handler: Handler): ROUTER = addHandler(BEFORE, path, handler)

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(handler: Handler): ROUTER = before("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(path: String, handler: Handler): ROUTER = addHandler(AFTER, path, handler)

    /**
     * Adds an AFTER request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(handler: Handler): ROUTER = after("*", handler)

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    override fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): ROUTER {
        internalRouter.wsException(exceptionClass, exceptionHandler)
        return getThis()
    }

    /**
     * Adds a WebSocket handler on the specified path.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>): ROUTER = ws(path, ws, *emptyArray())

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     * @see io.javalin.security.AccessManager
     */
    override fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): ROUTER {
        internalRouter.addWsHandler(WEBSOCKET, path, ws, *roles)
        return getThis()
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): ROUTER {
        internalRouter.addWsHandler(WS_BEFORE, path, wsConfig)
        return getThis()
    }

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): ROUTER = wsBefore("*", wsConfig)

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    override fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): ROUTER {
        internalRouter.addWsHandler(WS_AFTER, path, wsConfig)
        return getThis()
    }

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): ROUTER = wsAfter("*", wsConfig)

}
