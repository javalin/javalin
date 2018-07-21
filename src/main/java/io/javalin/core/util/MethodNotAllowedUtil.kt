package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher

object MethodNotAllowedUtil {

    fun findAvailableHttpHandlerTypes(matcher: PathMatcher, requestUri: String) =
            enumValues<HandlerType>().filter { it.isHttpMethod() && matcher.findEntries(it, requestUri).isNotEmpty() }

    fun getAvailableHandlerTypes(ctx: Context, availableHandlerTypes: List<HandlerType>): Map<String, String> {
        if (ctx.header(Header.ACCEPT)?.contains("text/html") == true) {
            return createHtmlMethodNotAllowed(availableHandlerTypes)
        }
        return createJsonMethodNotAllowed(availableHandlerTypes)
    }

    private fun createJsonMethodNotAllowed(availableHandlerTypes: List<HandlerType>) = mapOf(
            "availableMethods" to """${availableHandlerTypes.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")}}"""
    )

    private fun createHtmlMethodNotAllowed(availableHandlerTypes: List<HandlerType>) = mapOf(
            "Available methods" to availableHandlerTypes.joinToString(", ")
    )
}
