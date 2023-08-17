package io.javalin.router

import io.javalin.config.RouterConfig
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.router.matcher.PathParser
import io.javalin.security.RouteRole

data class HttpHandlerEntry(
    val type: HandlerType,
    val path: String,
    val routerConfig: RouterConfig,
    val roles: Set<RouteRole>,
    val handler: Handler,
) {
    private val pathParser = PathParser(path, routerConfig)
    fun matches(requestUri: String): Boolean = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String): Map<String, String> = pathParser.extractPathParams(requestUri)
    fun handle(ctx: JavalinServletContext, requestUri: String) = handler.handle(ctx.update(this, requestUri))
}
