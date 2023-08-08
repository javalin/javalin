package io.javalin.router

import io.javalin.config.JavalinConfig
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

class DefaultRouting(private val cfg: JavalinConfig) : DefaultRoutingApi<DefaultRouting, DefaultRouting> {

    companion object {
        @JvmStatic
        val Default: RouterFactory<DefaultRouting, DefaultRouting> = object : RouterFactory<DefaultRouting, DefaultRouting> {
            override fun create(cfg: JavalinConfig, internalRouter: InternalRouter<*>, setup: Consumer<DefaultRouting>): DefaultRouting {
                val javalinRouter = DefaultRouting(cfg)
                setup.accept(javalinRouter)
                return javalinRouter
            }
        }
    }

    override fun getCfg(): JavalinConfig = cfg

}

interface DefaultRoutingApi<API : RoutingApi<API, SETUP>, SETUP> : RoutingApi<API, SETUP> {

    fun getCfg(): JavalinConfig

    private val internalRouter: InternalRouter<*>
        get() = getCfg().pvt.internalRouter

    @Suppress("UNCHECKED_CAST")
    private fun getThis(): API = this as API

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): API {
        internalRouter.exception(exceptionClass, exceptionHandler)
        return getThis()
    }

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, handler: Handler): API = error(status.code, "*", handler)

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: Int, handler: Handler): API = error(status, "*", handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: HttpStatus, contentType: String, handler: Handler): API = error(status.code, contentType, handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    fun error(status: Int, contentType: String, handler: Handler): API {
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
    fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): API {
        internalRouter.addHandler(handlerType, path, handler, *roles)
        return getThis()
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun addHandler(httpMethod: HandlerType, path: String, handler: Handler): API = addHandler(httpMethod, path, handler, *emptyArray())

    /**
     * Adds a GET request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    operator fun get(path: String, handler: Handler): API = addHandler(GET, path, handler)

    /**
     * Adds a POST request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun post(path: String, handler: Handler): API = addHandler(POST, path, handler)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun put(path: String, handler: Handler): API = addHandler(PUT, path, handler)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun patch(path: String, handler: Handler): API = addHandler(PATCH, path, handler)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun delete(path: String, handler: Handler): API = addHandler(DELETE, path, handler)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun head(path: String, handler: Handler): API = addHandler(HEAD, path, handler)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    fun options(path: String, handler: Handler): API = addHandler(OPTIONS, path, handler)

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    operator fun get(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(GET, path, handler, *roles)

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun post(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(POST, path, handler, *roles)

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun put(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(PUT, path, handler, *roles)

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun patch(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(PATCH, path, handler, *roles)

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun delete(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(DELETE, path, handler, *roles)

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun head(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(HEAD, path, handler, *roles)

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * Requires an access manager to be set on the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    fun options(path: String, handler: Handler, vararg roles: RouteRole): API = addHandler(OPTIONS, path, handler, *roles)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>): API = sse(path, client, *emptyArray())

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, handler: SseHandler): API = get(path, handler)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     * Requires an access manager to be set on the instance.
     */
    fun sse(path: String, client: Consumer<SseClient>, vararg roles: RouteRole): API = get(path, SseHandler(clientConsumer = client), *roles)

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(path: String, handler: Handler): API = addHandler(BEFORE, path, handler)

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun before(handler: Handler): API = before("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(path: String, handler: Handler): API = addHandler(AFTER, path, handler)

    /**
     * Adds an AFTER request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation.before-handlers)
     */
    fun after(handler: Handler): API = after("*", handler)

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): API {
        internalRouter.wsException(exceptionClass, exceptionHandler)
        return getThis()
    }

    /**
     * Adds a WebSocket handler on the specified path.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>): API = ws(path, ws, *emptyArray())

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * Requires an access manager to be set on the instance.
     * See: [WebSockets in docs](https://javalin.io/documentation.websockets)
     * @see io.javalin.security.AccessManager
     */
    fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): API {
        internalRouter.addWsHandler(WEBSOCKET, path, ws, *roles)
        return getThis()
    }

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): API {
        internalRouter.addWsHandler(WS_BEFORE, path, wsConfig)
        return getThis()
    }

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): API = wsBefore("*", wsConfig)

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): API {
        internalRouter.addWsHandler(WS_AFTER, path, wsConfig)
        return getThis()
    }

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): API = wsAfter("*", wsConfig)

}
