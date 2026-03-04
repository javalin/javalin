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
        val pathParams = extractPathParams(requestUri)
        val updatedCtx = ctx.update(endpoint, pathParams)
        when (val handlerWrapper = routerConfig.handlerWrapper) {
            null -> endpoint.handler.handle(updatedCtx)
            else -> handlerWrapper.wrap(endpoint).handle(updatedCtx)
        }
    }

    fun matches(requestUri: String): Boolean =
        pathParser.matches(requestUri)

    fun extractPathParams(requestUri: String): Map<String, String> =
        pathParser.extractPathParams(requestUri)

    /** Matches and extracts path params in a single regex pass. Returns null if no match. */
    fun matchAndExtract(requestUri: String): Map<String, String>? =
        pathParser.matchAndExtract(requestUri)

}
