package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher

object MethodNotAllowedUtil {

    fun findAvailableHttpHandlerTypes(matcher: PathMatcher, requestUri: String) =
            enumValues<HandlerType>().filter { it.isHttpMethod() && matcher.findEntries(it, requestUri).isNotEmpty() }

    fun getAvailableHandlerTypes(ctx: Context, availableHandlerTypes: List<HandlerType>): Map<String, String> = mapOf(
            (if (ContextUtil.acceptsHtml(ctx)) "Available methods" else "availableMethods") to availableHandlerTypes.joinToString(", ")
    )
}
