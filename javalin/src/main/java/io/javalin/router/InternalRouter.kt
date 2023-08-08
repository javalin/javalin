package io.javalin.router

import io.javalin.config.JavalinConfig
import io.javalin.config.RoutingConfig
import io.javalin.event.EventManager
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.servlet.ErrorMapper
import io.javalin.http.servlet.ExceptionMapper
import io.javalin.router.matcher.PathMatcher
import io.javalin.security.RouteRole
import io.javalin.util.Util
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsRouter
import java.util.function.Consumer

open class InternalRouter(
    private val wsRouter: WsRouter,
    private val eventManager: EventManager,
    internal val routingConfig: RoutingConfig
) {

    open val pathMatcher = PathMatcher()
    open val exceptionMapper = ExceptionMapper()
    open val errorMapper = ErrorMapper()

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): InternalRouter = also {
        @Suppress("UNCHECKED_CAST")
        exceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
    }

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    open fun error(status: Int, contentType: String, handler: Handler): InternalRouter = also {
        errorMapper.addHandler(status, contentType, handler)
    }

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    open fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): InternalRouter = also {
        val roleSet = HashSet(roles.asList())
        pathMatcher.add(HandlerEntry(handlerType, path, routingConfig, roleSet, handler))
        eventManager.fireHandlerAddedEvent(
            HandlerMetaInfo(
                httpMethod = handlerType,
                path = Util.prefixContextPath(routingConfig.contextPath, path),
                handler = handler,
                roles = roleSet
            )
        )
    }

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): InternalRouter = also {
        @Suppress("UNCHECKED_CAST")
        wsRouter.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    open fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): InternalRouter = also {
        val roleSet = HashSet(roles.asList())
        wsRouter.addHandler(handlerType, path, wsConfig, roleSet)
        eventManager.fireWsHandlerAddedEvent(
            WsHandlerMetaInfo(
                handlerType = handlerType,
                path = Util.prefixContextPath(routingConfig.contextPath, path),
                wsConfig = wsConfig,
                roles = roleSet
            )
        )
    }

}
