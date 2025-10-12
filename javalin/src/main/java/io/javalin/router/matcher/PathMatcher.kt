package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.CustomHttpMethod
import io.javalin.router.ParsedEndpoint
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    private val handlerEntries: Map<HandlerType, MutableList<ParsedEndpoint>> =
        HandlerType.entries.associateWithTo(EnumMap(HandlerType::class.java)) { arrayListOf() }

    fun add(entry: ParsedEndpoint) {
        val type = entry.endpoint.method
        val path = entry.endpoint.path

        if (type.isHttpMethod && handlerEntries[type]!!.find { it.endpoint.method == type && it.endpoint.path == path } != null) {
            throw IllegalArgumentException("Handler with type='${type}' and path='${path}' already exists.")
        }

        handlerEntries[type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<ParsedEndpoint> =
        when (requestUri) {
            null -> handlerEntries[handlerType]!!.stream()
            else -> handlerEntries[handlerType]!!.stream().filter { he -> match(he, requestUri) }
        }

    /**
     * Finds handlers for a custom HTTP method by checking entries under INVALID with matching CustomHttpMethod metadata.
     */
    fun findEntries(handlerType: HandlerType, requestUri: String?, customMethodName: String?): Stream<ParsedEndpoint> {
        if (handlerType != HandlerType.INVALID || customMethodName == null) {
            return findEntries(handlerType, requestUri)
        }
        
        // For custom methods, filter by CustomHttpMethod metadata
        val methodUpper = customMethodName.uppercase()
        val filtered = handlerEntries[HandlerType.INVALID]!!
            .filter { it.endpoint.metadata(CustomHttpMethod::class.java)?.methodName == methodUpper }
        
        return when (requestUri) {
            null -> filtered.stream()
            else -> filtered.stream().filter { he -> match(he, requestUri) }
        }
    }

    fun allEntries() = handlerEntries.values.flatten()

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean =
        handlerEntries[handlerType]!!.any { entry -> match(entry, requestUri) }

    /**
     * Checks if there are handlers for a custom HTTP method.
     */
    internal fun hasEntries(handlerType: HandlerType, requestUri: String, customMethodName: String?): Boolean {
        if (handlerType != HandlerType.INVALID || customMethodName == null) {
            return hasEntries(handlerType, requestUri)
        }
        
        val methodUpper = customMethodName.uppercase()
        return handlerEntries[HandlerType.INVALID]!!.any { entry ->
            entry.endpoint.metadata(CustomHttpMethod::class.java)?.methodName == methodUpper && match(entry, requestUri)
        }
    }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
