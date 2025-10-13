package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.CustomHttpMethod
import io.javalin.router.ParsedEndpoint
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    // Use LinkedHashMap to maintain insertion order (enum order for standard methods, then custom methods)
    private val handlerEntries: MutableMap<String, MutableList<ParsedEndpoint>> = LinkedHashMap()

    init {
        // Pre-populate with standard HTTP methods from enum in order
        HandlerType.entries.forEach { handlerType ->
            handlerEntries[handlerType.name] = mutableListOf()
        }
    }

    fun add(entry: ParsedEndpoint) {
        val methodKey = getMethodKey(entry.endpoint.method, entry.endpoint.metadata(CustomHttpMethod::class.java))
        val path = entry.endpoint.path

        // Check for duplicates for HTTP methods
        val entries = handlerEntries.computeIfAbsent(methodKey) { mutableListOf() }
        val handlerType = entry.endpoint.method
        if (handlerType.isHttpMethod && entries.find { it.endpoint.path == path } != null) {
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

    fun findEntries(handlerType: HandlerType, requestUri: String?, customMethodName: String?): Stream<ParsedEndpoint> {
        // If custom method name is provided, use it; otherwise use handler type name
        val methodKey = customMethodName?.uppercase() ?: handlerType.name
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

    internal fun hasEntries(handlerType: HandlerType, requestUri: String, customMethodName: String?): Boolean {
        val methodKey = customMethodName?.uppercase() ?: handlerType.name
        val entries = handlerEntries[methodKey] ?: return false
        return entries.any { entry -> match(entry, requestUri) }
    }

    private fun getMethodKey(handlerType: HandlerType, customMethod: CustomHttpMethod?): String {
        return customMethod?.methodName ?: handlerType.name
    }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
