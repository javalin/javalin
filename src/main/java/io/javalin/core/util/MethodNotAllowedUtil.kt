package io.javalin.core.util

import io.javalin.core.HandlerType
import io.javalin.core.PathMatcher
import io.javalin.translator.json.JavalinJsonPlugin.objectToJsonMapper

private data class JsonMethodNotAllowed(val availableMethods: List<HandlerType>)

object MethodNotAllowedUtil {

    @JvmStatic
    fun findAvailableHandlerTypes(matcher: PathMatcher, requestUri: String): List<HandlerType> {
        val availableHandlerTypes = ArrayList<HandlerType>()

        enumValues<HandlerType>().forEach {
            val entries = matcher.findEntries(it, requestUri)

            if (!entries.isEmpty()) {
                availableHandlerTypes.add(it)
            }
        }
        return availableHandlerTypes
    }

    fun createJsonMethodNotAllowed(availableHandlerTypes: List<HandlerType>): String {

        return """{"availableMethods":${availableHandlerTypes.joinToString(separator = "\", \"", prefix = "[\"", postfix = "\"]")}}"""
    }

    @JvmStatic
    fun createHtmlMethodNotAllowed(availableHandlerTypes: List<HandlerType>): String {
        return """
                    <!DOCTYPE html>
                    <html lang="en">
                        <head>
                            <meta charset="UTF-8">
                            <title>Method Not Allowed</title>
                        </head>
                        <body>
                            <h1>405 - Method Not Allowed</h1>
                            <p>
                                Available Methods: <strong>${availableHandlerTypes.joinToString(", ")}</strong>
                            </p>
                        </body>
                    </html>
                """
    }
}
