/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.HandlerEntry
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    private val handlerEntries: Map<HandlerType, MutableList<HandlerEntry>> =
        HandlerType.values().associateWithTo(EnumMap(HandlerType::class.java)) { arrayListOf() }

    fun add(entry: HandlerEntry) {
        if (entry.type.isHttpMethod && handlerEntries[entry.type]!!.find { it.type == entry.type && it.path == entry.path } != null) {
            throw IllegalArgumentException("Handler with type='${entry.type}' and path='${entry.path}' already exists.")
        }
        handlerEntries[entry.type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<HandlerEntry> =
        when (requestUri) {
            null -> handlerEntries[handlerType]!!.stream()
            else -> handlerEntries[handlerType]!!.stream().filter { he -> match(he, requestUri) }
        }

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean =
        handlerEntries[handlerType]!!.any { entry -> match(entry, requestUri) }

    private fun match(entry: HandlerEntry, requestPath: String): Boolean = when (entry.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
