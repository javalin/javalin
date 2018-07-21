package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher

object MethodNotAllowedUtil {

    fun findAvailableHttpHandlerTypes(matcher: PathMatcher, requestUri: String) =
            enumValues<HandlerType>().filter { it.isHttpMethod() && matcher.findEntries(it, requestUri).isNotEmpty() }

    fun getAvailableHandlerTypes(ctx: Context, availableHandlerTypes: List<HandlerType>): Map<String, String> {
        if (ctx.header(Header.ACCEPT)?.contains("text/html") == true) {
            return mapOf("Available methods" to availableHandlerTypes.joinToString(", "))
        }
        return mapOf("availableMethods" to availableHandlerTypes.joinToString(", "))
    }
}
