package io.javalin.router

import io.javalin.config.RouterConfig
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.router.matcher.PathParser

class ParsedEndpoint(
    @JvmField val endpoint: Endpoint,
    private val routerConfig: RouterConfig,
) {

    private val pathParser = PathParser(endpoint.path, routerConfig)

    fun handle(ctx: JavalinServletContext, requestUri: String) {
        val endpoint = endpoint.withMetadata(PathParams(extractPathParams(requestUri)))
        val updatedCtx = ctx.update(endpoint)
        when (routerConfig.endpointWrapper) {
            null -> endpoint.handler.handle(updatedCtx)
            else -> routerConfig.endpointWrapper?.invoke(endpoint)?.handle(updatedCtx)
        }
    }

    fun matches(requestUri: String): Boolean =
        pathParser.matches(requestUri)

    fun extractPathParams(requestUri: String): Map<String, String> =
        pathParser.extractPathParams(requestUri)

}
