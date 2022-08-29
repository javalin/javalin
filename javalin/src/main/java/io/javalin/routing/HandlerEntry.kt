package io.javalin.routing

import io.javalin.config.RoutingConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.security.RouteRole

data class HandlerEntry(
    val type: HandlerType,
    val path: String,
    val routingConfig: RoutingConfig,
    val roles: Set<RouteRole>,
    val handler: Handler,
) {
    private val pathParser = PathParser(path, routingConfig)
    fun matches(requestUri: String): Boolean = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String): Map<String, String> = pathParser.extractPathParams(requestUri)
    fun handle(ctx: JavalinServletContext, requestUri: String) = handler.handle(ctx.update(this, requestUri))
}
