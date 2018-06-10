package io.javalin.core.util

import io.javalin.Context
import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher

object MethodNotAllowedUtil {

    fun findAvailableHttpHandlerTypes(matcher: PathMatcher, requestUri: String) =
            enumValues<HandlerType>().filter { it.isHttpMethod() && matcher.findEntries(it, requestUri).isNotEmpty() }

    fun getAvailableHandlerTypes(ctx: Context, availableHandlerTypes: List<HandlerType>): String {
        if (ctx.header(Header.ACCEPT)?.contains("text/html") == true) {
            return createHtmlMethodNotAllowed(availableHandlerTypes)
        }
        return createJsonMethodNotAllowed(availableHandlerTypes)
    }

    private fun createJsonMethodNotAllowed(availableHandlerTypes: List<HandlerType>) =
            """{"availableMethods":${availableHandlerTypes.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")}}"""

    private fun createHtmlMethodNotAllowed(availableHandlerTypes: List<HandlerType>) =
            """|<!DOCTYPE html>
               |<html lang="en">
               |    <head>
               |        <meta charset="UTF-8">
               |        <title>Method Not Allowed</title>
               |    </head>
               |    <body>
               |        <h1>405 - Method Not Allowed</h1>
               |        <p>
               |            Available Methods: <strong>${availableHandlerTypes.joinToString(", ")}</strong>
               |        </p>
               |    </body>
               |</html>""".trimMargin()
}
