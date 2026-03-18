/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint
import java.util.Collections

class PathMatcher {

    private val handlerEntries: MutableMap<HandlerType, MutableList<ParsedEndpoint>> =
        HandlerType.values().associateWithTo(mutableMapOf()) { arrayListOf() }

    private fun handlerEntries(handlerType: HandlerType) = handlerEntries[handlerType] ?: emptyList()

    fun add(entry: ParsedEndpoint) {
        val type = entry.endpoint.method
        val path = entry.endpoint.path

        handlerEntries.putIfAbsent(type, arrayListOf()) // Ensure the handler type exists in the map (for user-defined http methods)

        if (type.isHttpMethod && handlerEntries[type]!!.find { it.endpoint.method == type && it.endpoint.path == path } != null) {
            throw IllegalArgumentException("Handler with type='${type}' and path='${path}' already exists.")
        }

        handlerEntries[type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): List<ParsedEndpoint> {
        if (requestUri == null) return Collections.unmodifiableList(handlerEntries(handlerType))
        val entries = handlerEntries(handlerType)
        // Optimized filtering: avoid allocating a list for common cases (0 or 1 match)
        var firstMatch: ParsedEndpoint? = null
        var result: MutableList<ParsedEndpoint>? = null
        for (entry in entries) {
            if (match(entry, requestUri)) {
                if (firstMatch == null) {
                    firstMatch = entry
                } else {
                    if (result == null) {
                        result = ArrayList(4)
                        result.add(firstMatch)
                    }
                    result.add(entry)
                }
            }
        }
        return result ?: (if (firstMatch != null) listOf(firstMatch) else emptyList())
    }

    fun findFirstEntry(handlerType: HandlerType, requestUri: String): ParsedEndpoint? {
        for (entry in handlerEntries(handlerType)) {
            if (match(entry, requestUri)) return entry
        }
        return null
    }

    fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean =
        findFirstEntry(handlerType, requestUri) != null

    fun allEntries() = handlerEntries.values.flatten()

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
