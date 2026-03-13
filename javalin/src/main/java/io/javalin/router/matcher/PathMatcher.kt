/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint
import java.util.stream.Stream

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

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<ParsedEndpoint> =
        when (requestUri) {
            null -> handlerEntries(handlerType).stream()
            else -> handlerEntries(handlerType).stream().filter { he -> match(he, requestUri) }
        }

    fun allEntries() = handlerEntries.values.flatten()

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean =
        handlerEntries(handlerType).any { entry -> match(entry, requestUri) }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean = when (entry.endpoint.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
