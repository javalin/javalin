package io.javalin.router

import io.javalin.config.JettyConfig
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
import io.javalin.security.Roles
import io.javalin.security.RouteRole
import io.javalin.util.Util
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsHandlerEntry
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsRouter
import jakarta.servlet.http.HttpServletResponse
import java.util.function.Consumer
import java.util.stream.Stream

open class InternalRouter(
    private val wsRouter: WsRouter,
    private val eventManager: EventManager,
    private val routerConfig: RouterConfig,
    jettyConfig: JettyConfig
) {

    protected open val httpPathMatcher = PathMatcher()
    protected open val httpErrorMapper = ErrorMapper()
    protected open val httpExceptionMapper = ExceptionMapper(routerConfig, jettyConfig)

    /**
     * Adds a request handler for the specified handlerType and path to the instance.
     * This is the method that all the verb-methods (get/post/put/etc) call.
     * See: [Handlers in docs](https://javalin.io/documentation.handlers)
     */
    open fun addHttpEndpoint(endpoint: Endpoint): InternalRouter {
        httpPathMatcher.add(ParsedEndpoint(endpoint, routerConfig))
        eventManager.fireHandlerAddedEvent(
            HandlerMetaInfo(
                httpMethod = endpoint.method,
                path = Util.prefixContextPath(routerConfig.contextPath, endpoint.path),
                handler = endpoint.handler,
                roles = endpoint.metadata(Roles::class.java)?.roles ?: emptySet()
            )
        )
        return this
    }

    /**
     * Get a list of all registered HTTP handlers.
     */
    fun allHttpHandlers(): List<ParsedEndpoint> = httpPathMatcher.allEntries()

    /**
     * Checks if the instance has a handler for the specified handlerType and path.
     */
    open fun hasHttpHandlerEntry(handlerType: HandlerType, requestUri: String): Boolean =
        httpPathMatcher.hasEntries(handlerType, requestUri)

    /**
     * Finds all matching handlers for the specified handlerType and path.
     * @return a handler for the specified handlerType and path, or null if no handler is found
     */
    open fun findHttpHandlerEntries(handlerType: HandlerType, requestUri: String? = null): Stream<ParsedEndpoint> =
        httpPathMatcher.findEntries(handlerType, requestUri)

    /**
     * Adds an error mapper for the specified content-type to the instance.
     * Useful for turning error-codes (404, 500) into standardized messages/pages
     * See: [Error mapping in docs](https://javalin.io/documentation.error-mapping)
     */
    open fun addHttpErrorHandler(status: Int, contentType: String, handler: Handler): InternalRouter {
        httpErrorMapper.addHandler(status, contentType, handler)
        return this
    }

    /**
     * Handles an error by looking up the correct error mapper and executing it.
     */
    open fun handleHttpError(statusCode: Int, ctx: Context): Unit =
        httpErrorMapper.handle(statusCode, ctx)

    /**
     * Adds an exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> addHttpExceptionHandler(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): InternalRouter {
        @Suppress("UNCHECKED_CAST")
        httpExceptionMapper.handlers[exceptionClass] = exceptionHandler as ExceptionHandler<Exception>
        return this
    }

    /**
     * Handles an exception by looking up the correct exception mapper and executing it.
     */
    open fun handleHttpException(ctx: Context, throwable: Throwable): Unit =
        httpExceptionMapper.handle(ctx, throwable)

    /**
     * Handles an unexpected throwable by looking up the correct exception mapper and executing it.
     */
    open fun handleHttpUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable): Nothing? =
        httpExceptionMapper.handleUnexpectedThrowable(res, throwable)

    /**
     * Adds a WebSocket exception mapper to the instance.
     * See: [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
     */
    open fun <E : Exception> addWsExceptionHandler(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): InternalRouter {
        @Suppress("UNCHECKED_CAST")
        wsRouter.wsExceptionMapper.handlers[exceptionClass] = exceptionHandler as WsExceptionHandler<Exception>
        return this
    }

    /**
     * Adds a specific WebSocket handler for the given path to the instance.
     */
    open fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): InternalRouter {
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
        return this
    }

    /**
     * Get a list of all registered WebSocket handlers.
     */
    fun allWsHandlers(): List<WsHandlerEntry> = wsRouter.wsPathMatcher.allEntries()

}
