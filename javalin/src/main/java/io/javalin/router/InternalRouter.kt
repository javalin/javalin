package io.javalin.router

import io.javalin.config.RouterConfig
import io.javalin.event.EventManager
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.router.error.ErrorMapper
import io.javalin.router.exception.ExceptionMapper
import io.javalin.router.matcher.PathMatcher
import io.javalin.security.RouteRole
import io.javalin.util.Util
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsRouter
import jakarta.servlet.http.HttpServletResponse
import java.util.function.Consumer
import java.util.stream.Stream

open class InternalRouter<IR : InternalRouter<IR>>(
    private val wsRouter: WsRouter,
    private val eventManager: EventManager,
    internal val routerConfig: RouterConfig
) {

    protected open val pathMatcher = PathMatcher()
    protected open val errorMapper = ErrorMapper()
    protected open val exceptionMapper = ExceptionMapper(routerConfig)

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * Requires an access manager to be set on the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     * @see io.javalin.security.AccessManager
     */
    open fun addHandler(handlerType: HandlerType, path: String, handler: Handler, vararg roles: RouteRole): IR {
        val roleSet = HashSet(roles.asList())
        pathMatcher.add(HandlerEntry(handlerType, path, routerConfig, roleSet, handler))
        eventManager.fireHandlerAddedEvent(
            HandlerMetaInfo(
                httpMethod = handlerType,
                path = Util.prefixContextPath(routerConfig.contextPath, path),
                handler = handler,
                roles = roleSet
            )
        )
        return getThis()
    }

    /**
     * Checks if the instance has a handler for the specified handlerType and path.
     */
    open fun hasHandlerEntry(handlerType: HandlerType, requestUri: String): Boolean =
        pathMatcher.hasEntries(handlerType, requestUri)

    /**
     * Finds all matching handlers for the specified handlerType and path.
     * @return a handler for the specified handlerType and path, or null if no handler is found
     */
    open fun findHandlerEntries(handlerType: HandlerType, requestUri: String? = null): Stream<HandlerEntry> =
        pathMatcher.findEntries(handlerType, requestUri)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    open fun error(status: Int, contentType: String, handler: Handler): IR {
        errorMapper.addHandler(status, contentType, handler)
        return getThis()
    }

    /**
     * Handles an error by looking up the correct error mapper and executing it.
     */
    open fun handleError(statusCode: Int, ctx: Context): Unit =
        errorMapper.handle(statusCode, ctx)

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): IR {
        @Suppress("UNCHECKED_CAST")
        exceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
        return getThis()
    }

    /**
     * Handles an exception by looking up the correct exception mapper and executing it.
     */
    open fun handleException(ctx: Context, throwable: Throwable): Unit =
        exceptionMapper.handle(ctx, throwable)

    /**
     * Handles an unexpected throwable by looking up the correct exception mapper and executing it.
     */
    open fun handleUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable): Nothing? =
        exceptionMapper.handleUnexpectedThrowable(res, throwable)

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): IR {
        @Suppress("UNCHECKED_CAST")
        wsRouter.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
        return getThis()
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     * Requires an access manager to be set on the instance.
     */
    open fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): IR {
        val roleSet = HashSet(roles.asList())
        wsRouter.addHandler(handlerType, path, wsConfig, roleSet)
        eventManager.fireWsHandlerAddedEvent(
            WsHandlerMetaInfo(
                handlerType = handlerType,
                path = Util.prefixContextPath(routerConfig.contextPath, path),
                wsConfig = wsConfig,
                roles = roleSet
            )
        )
        return getThis()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getThis(): IR = this as IR

}
