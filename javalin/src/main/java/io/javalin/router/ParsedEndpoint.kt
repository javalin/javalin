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
        val pathParams = PathParams(extractPathParams(requestUri))
        val enrichedEndpoint = endpoint.withMetadata(pathParams)
        val updatedCtx = ctx.update(enrichedEndpoint)
        when (val handlerWrapper = routerConfig.handlerWrapper) {
            null -> enrichedEndpoint.handler.handle(updatedCtx)
            else -> handlerWrapper.wrap(enrichedEndpoint).handle(updatedCtx)
        }
    }

    fun matches(requestUri: String): Boolean =
        pathParser.matches(requestUri)

    fun extractPathParams(requestUri: String): Map<String, String> =
        pathParser.extractPathParams(requestUri)

}
