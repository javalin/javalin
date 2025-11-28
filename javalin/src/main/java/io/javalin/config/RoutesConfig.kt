@file:Suppress("internal", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package io.javalin.config

import io.javalin.apibuilder.ApiBuilder
import io.javalin.apibuilder.EndpointGroup
import io.javalin.http.ExceptionHandler
import io.javalin.http.Handler
import io.javalin.router.Endpoint
import io.javalin.router.JavalinDefaultRouting
import io.javalin.router.JavalinDefaultRoutingApi
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionHandler
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

/**
 * Configuration for Routes - provides direct access to HTTP verbs and routing methods.
 * This is the new preferred way to define routes in Javalin 7.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinState.routes]
 */
class RoutesConfig(internal val cfg: JavalinState) : JavalinDefaultRoutingApi<RoutesConfig> {

    override fun <E : Exception> exception(exceptionClass: Class<E>, exceptionHandler: ExceptionHandler<in E>): RoutesConfig = apply {
        cfg.internalRouter.addHttpExceptionHandler(exceptionClass, exceptionHandler)
    }

    override fun error(status: Int, contentType: String, handler: Handler): RoutesConfig = apply {
        cfg.internalRouter.addHttpErrorHandler(status, contentType, handler)
    }

    override fun addEndpoint(endpoint: Endpoint): RoutesConfig = apply {
        cfg.internalRouter.addHttpEndpoint(endpoint)
    }

    override fun <E : Exception> wsException(exceptionClass: Class<E>, exceptionHandler: WsExceptionHandler<in E>): RoutesConfig = apply {
        cfg.internalRouter.addWsExceptionHandler(exceptionClass, exceptionHandler)
    }

    override fun addWsHandler(handlerType: WsHandlerType, path: String, wsConfig: Consumer<WsConfig>, vararg roles: RouteRole): RoutesConfig = apply {
        cfg.internalRouter.addWsHandler(handlerType, path, wsConfig, *roles)
    }

    /**
     * Adds routes using the ApiBuilder DSL.
     * This method allows you to define routes using the static methods from ApiBuilder.
     *
     * @param endpoints the endpoint group containing the route definitions
     * @return this RoutesConfig instance for method chaining
     */
    fun apiBuilder(endpoints: EndpointGroup): RoutesConfig = apply {
        try {
            ApiBuilder.setStaticJavalin(JavalinDefaultRouting(cfg))
            endpoints.addEndpoints()
        } finally {
            ApiBuilder.clearStaticJavalin()
        }
    }

}
