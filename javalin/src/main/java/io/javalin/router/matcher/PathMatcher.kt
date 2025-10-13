package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    // Use LinkedHashMap to maintain insertion order (standard methods first, then custom methods)
    private val handlerEntries: MutableMap<String, MutableList<ParsedEndpoint>> = LinkedHashMap()

    init {
        // Pre-populate with standard HTTP methods and lifecycle handlers from enum in order
        HandlerType.entries.forEach { handlerType ->
            handlerEntries[handlerType.name] = mutableListOf()
        }
    }

    fun add(entry: ParsedEndpoint) {
        val methodKey = entry.endpoint.method
        val path = entry.endpoint.path

        // Check for duplicates for HTTP methods
        val entries = handlerEntries.computeIfAbsent(methodKey) { mutableListOf() }
        // Only check duplicates for actual HTTP methods (not lifecycle handlers like BEFORE, AFTER)
        val isHttpMethod = try { HandlerType.valueOf(methodKey).isHttpMethod } catch (e: IllegalArgumentException) { true }
        if (isHttpMethod && entries.find { it.endpoint.path == path } != null) {
            throw IllegalArgumentException("Handler with method='${methodKey}' and path='${path}' already exists.")
        }

        entries.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<ParsedEndpoint> {
        val methodKey = handlerType.name
        val entries = handlerEntries[methodKey] ?: return Stream.empty()
        
        return when (requestUri) {
            null -> entries.stream()
            else -> entries.stream().filter { he -> match(he, requestUri) }
        }
    }

    fun findEntries(method: String, requestUri: String?): Stream<ParsedEndpoint> {
        val methodKey = method.uppercase()
        val entries = handlerEntries[methodKey] ?: return Stream.empty()
        
        return when (requestUri) {
            null -> entries.stream()
            else -> entries.stream().filter { he -> match(he, requestUri) }
        }
    }

    fun allEntries() = handlerEntries.values.flatten()

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean {
        val methodKey = handlerType.name
        val entries = handlerEntries[methodKey] ?: return false
        return entries.any { entry -> match(entry, requestUri) }
    }

    internal fun hasEntries(method: String, requestUri: String): Boolean {
        val methodKey = method.uppercase()
        val entries = handlerEntries[methodKey] ?: return false
        return entries.any { entry -> match(entry, requestUri) }
    }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
