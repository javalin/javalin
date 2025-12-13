package io.javalin.router

import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.AFTER
import io.javalin.http.HandlerType.AFTER_MATCHED
import io.javalin.http.HandlerType.BEFORE
import io.javalin.http.HandlerType.BEFORE_MATCHED
import io.javalin.http.HandlerType.DELETE
import io.javalin.http.HandlerType.GET
import io.javalin.http.HandlerType.HEAD
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.HandlerType.PATCH
import io.javalin.http.HandlerType.POST
import io.javalin.http.HandlerType.PUT
import io.javalin.http.HandlerType.WEBSOCKET_AFTER_UPGRADE
import io.javalin.http.HandlerType.WEBSOCKET_BEFORE_UPGRADE
import io.javalin.http.HttpStatus
import io.javalin.http.sse.SseClient
import io.javalin.http.sse.SseHandler
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsHandlerType.WEBSOCKET
import io.javalin.websocket.WsHandlerType.WEBSOCKET_AFTER
import io.javalin.websocket.WsHandlerType.WEBSOCKET_BEFORE
import java.util.function.Consumer

/**
 * Default routing API providing all standard HTTP verbs, lifecycle handlers, and WebSocket support.
 * Provides default implementations for convenience methods, requiring implementers to only define:
 * - exception(), error(), addEndpoint(), wsException(), addWsHandler()
 */
interface JavalinDefaultRoutingApi {

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation#exception-mapping)
     */
    fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): JavalinDefaultRoutingApi

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation#error-mapping)
     */
    fun error(status: HttpStatus, handler: Handler): JavalinDefaultRoutingApi = error(status.code, "*", handler)

    /**
     * Adds an error mapper to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation#error-mapping)
     */
    fun error(status: Int, handler: Handler): JavalinDefaultRoutingApi = error(status, "*", handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation#error-mapping)
     */
    fun error(status: HttpStatus, contentType: String, handler: Handler): JavalinDefaultRoutingApi = error(status.code, contentType, handler)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation#error-mapping)
     */
    fun error(status: Int, contentType: String, handler: Handler): JavalinDefaultRoutingApi

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun addHttpHandler(httpMethod: HandlerType, path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(httpMethod, path, handler, *emptyArray())

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun addHttpHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi =
        addEndpoint(
            Endpoint.create(handlerType, path)
                .addMetadata(Roles(roles.toSet()))
                .handler(handler)
        )

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun addEndpoint(endpoint: Endpoint): JavalinDefaultRoutingApi

    /**
     * Adds a GET request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun get(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(GET, path, handler)

    /**
     * Adds a POST request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun post(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(POST, path, handler)

    /**
     * Adds a PUT request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun put(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(PUT, path, handler)

    /**
     * Adds a PATCH request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun patch(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(PATCH, path, handler)

    /**
     * Adds a DELETE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun delete(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(DELETE, path, handler)

    /**
     * Adds a HEAD request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun head(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(HEAD, path, handler)

    /**
     * Adds a OPTIONS request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun options(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(OPTIONS, path, handler)

    /**
     * Adds a GET request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun get(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(GET, path, handler, *roles)

    /**
     * Adds a POST request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun post(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(POST, path, handler, *roles)

    /**
     * Adds a PUT request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun put(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(PUT, path, handler, *roles)

    /**
     * Adds a PATCH request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun patch(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(PATCH, path, handler, *roles)

    /**
     * Adds a DELETE request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun delete(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(DELETE, path, handler, *roles)

    /**
     * Adds a HEAD request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun head(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(HEAD, path, handler, *roles)

    /**
     * Adds a OPTIONS request handler with the given roles for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#handlers)
     */
    fun options(path: String, handler: Handler, vararg roles: RouteRole): JavalinDefaultRoutingApi = addHttpHandler(OPTIONS, path, handler, *roles)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>): JavalinDefaultRoutingApi = sse(path, client, *emptyArray())

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, handler: SseHandler): JavalinDefaultRoutingApi = get(path, handler)

    /**
     * Adds a lambda handler for a Server Sent Event connection on the specified path.
     */
    fun sse(path: String, client: Consumer<SseClient>, vararg roles: RouteRole): JavalinDefaultRoutingApi = get(path, SseHandler(clientConsumer = client), *roles)

    /**
     * Adds a BEFORE request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun before(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(BEFORE, path, handler)

    /**
     * Adds a BEFORE request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun before(handler: Handler): JavalinDefaultRoutingApi = before("*", handler)

    /**
     * Adds a BEFORE_MATCHED request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun beforeMatched(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(BEFORE_MATCHED, path, handler)

    /**
     * Adds a BEFORE_MATCHED request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun beforeMatched(handler: Handler): JavalinDefaultRoutingApi = beforeMatched("*", handler)

    /**
     * Adds an AFTER request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun after(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(AFTER, path, handler)

    /**
     * Adds an AFTER request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun after(handler: Handler): JavalinDefaultRoutingApi = after("*", handler)

    /**
     * Adds an AFTER_MATCHED request handler for the specified path to the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun afterMatched(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(AFTER_MATCHED, path, handler)

    /**
     * Adds an AFTER_MATCHED request handler for all routes in the instance.
     * See: [Handlers in docs](https://javalin.io/documentation#before-handlers)
     */
    fun afterMatched(handler: Handler): JavalinDefaultRoutingApi = afterMatched("*", handler)

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation#exception-mapping)
     */
    fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): JavalinDefaultRoutingApi

    /**
     * Adds a WebSocket handler of the specified type on the specified path.
     * See: [WebSockets in docs](https://javalin.io/documentation#websockets)
     */
    fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): JavalinDefaultRoutingApi

    /**
     * Adds a WebSocket handler on the specified path.
     * See: [WebSockets in docs](https://javalin.io/documentation#websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>): JavalinDefaultRoutingApi = ws(path, ws, *emptyArray())

    /**
     * Adds a WebSocket handler on the specified path with the specified roles.
     * See: [WebSockets in docs](https://javalin.io/documentation#websockets)
     */
    fun ws(path: String, ws: Consumer<WsConfig>, vararg roles: RouteRole): JavalinDefaultRoutingApi = addWsHandler(WEBSOCKET, path, ws, *roles)

    /**
     * Adds a WebSocket before handler for the specified path to the instance.
     */
    fun wsBefore(path: String, wsConfig: Consumer<WsConfig>): JavalinDefaultRoutingApi = addWsHandler(WEBSOCKET_BEFORE, path, wsConfig)

    /**
     * Adds a WebSocket before handler for all routes in the instance.
     */
    fun wsBefore(wsConfig: Consumer<WsConfig>): JavalinDefaultRoutingApi = wsBefore("*", wsConfig)

    /**
     * Adds a WebSocket before upgrade handler for the specified path to the instance.
     */
    fun wsBeforeUpgrade(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(WEBSOCKET_BEFORE_UPGRADE, path, handler)

    /**
     * Adds a WebSocket before upgrade handler for all routes in the instance.
     */
    fun wsBeforeUpgrade(handler: Handler): JavalinDefaultRoutingApi = wsBeforeUpgrade("*", handler)

    /**
     * Adds a WebSocket after handler for the specified path to the instance.
     */
    fun wsAfter(path: String, wsConfig: Consumer<WsConfig>): JavalinDefaultRoutingApi = addWsHandler(WEBSOCKET_AFTER, path, wsConfig)

    /**
     * Adds a WebSocket after handler for all routes in the instance.
     */
    fun wsAfter(wsConfig: Consumer<WsConfig>): JavalinDefaultRoutingApi = wsAfter("*", wsConfig)

    /**
     * Adds a WebSocket after upgrade handler for the specified path to the instance.
     */
    fun wsAfterUpgrade(path: String, handler: Handler): JavalinDefaultRoutingApi = addHttpHandler(WEBSOCKET_AFTER_UPGRADE, path, handler)

    /**
     * Adds a WebSocket after upgrade handler for all routes in the instance.
     */
    fun wsAfterUpgrade(handler: Handler): JavalinDefaultRoutingApi = wsAfterUpgrade("*", handler)


}
