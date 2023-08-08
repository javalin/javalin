package io.javalin.http.router

import io.javalin.config.JavalinConfig
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.routing.HandlerEntry
import io.javalin.security.RouteRole
import io.javalin.util.Util
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

open class InternalRouter(internal val cfg: JavalinConfig) {

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): InternalRouter = also {
        @Suppress("UNCHECKED_CAST")
        cfg.pvt.exceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
    }

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    open fun error(status: Int, contentType: String, handler: Handler): InternalRouter = also {
        cfg.pvt.errorMapper.addHandler(status, contentType, handler)
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
        cfg.pvt.pathMatcher.add(HandlerEntry(handlerType, path, cfg.routing, roleSet, handler))
        cfg.pvt.eventManager.fireHandlerAddedEvent(
            HandlerMetaInfo(
                httpMethod = handlerType,
                path = Util.prefixContextPath(cfg.routing.contextPath, path),
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
        cfg.pvt.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    open fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): InternalRouter = also {
        val roleSet = HashSet(roles.asList())
        cfg.pvt.cfg.jetty.addHandler(handlerType, path, wsConfig, roleSet)
        cfg.pvt.eventManager.fireWsHandlerAddedEvent(
            WsHandlerMetaInfo(
                handlerType = handlerType,
                path = Util.prefixContextPath(cfg.routing.contextPath, path),
                wsConfig = wsConfig,
                roles = roleSet
            )
        )
    }

}
