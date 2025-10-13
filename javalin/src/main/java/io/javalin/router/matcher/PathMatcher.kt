package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    // Use LinkedHashMap to maintain insertion order (standard methods first, then custom methods)
    private val handlerEntries: MutableMap<HandlerType, MutableList<ParsedEndpoint>> = LinkedHashMap()

    init {
        // Pre-populate with standard HTTP methods and lifecycle handlers
        HandlerType.values().forEach { handlerType ->
            handlerEntries[handlerType] = mutableListOf()
        }
    }

    fun add(entry: ParsedEndpoint) {
        val method = entry.endpoint.method
        val path = entry.endpoint.path

        // Check for duplicates for HTTP methods
        val entries = handlerEntries.computeIfAbsent(method) { mutableListOf() }
        // Only check duplicates for actual HTTP methods (not lifecycle handlers like BEFORE, AFTER)
        if (method.isHttpMethod && entries.find { it.endpoint.path == path } != null) {
            throw IllegalArgumentException("Handler with method='${method}' and path='${path}' already exists.")
        }

        entries.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<ParsedEndpoint> {
        val entries = handlerEntries[handlerType] ?: return Stream.empty()
        
        return when (requestUri) {
            null -> entries.stream()
            else -> entries.stream().filter { he -> match(he, requestUri) }
        }
    }

    fun allEntries() = handlerEntries.values.flatten()

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean {
        val entries = handlerEntries[handlerType] ?: return false
        return entries.any { entry -> match(entry, requestUri) }
    }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
