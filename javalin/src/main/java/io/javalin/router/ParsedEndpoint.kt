package io.javalin.router

import io.javalin.config.RouterConfig
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.router.matcher.PathParser

class ParsedEndpoint(
    @JvmField val endpoint: Endpoint,
    routerConfig: RouterConfig,
) {

    private val pathParser = PathParser(endpoint.path, routerConfig)

    fun handle(ctx: JavalinServletContext, requestUri: String) {
        endpoint.handle(ctx.update(endpoint.withMetadata(PathParams(extractPathParams(requestUri)))))
    }

    fun matches(requestUri: String): Boolean =
        pathParser.matches(requestUri)

    fun extractPathParams(requestUri: String): Map<String, String> =
        pathParser.extractPathParams(requestUri)

}
