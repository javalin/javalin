/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.PathParser
import java.util.*

data class HandlerEntry(val type: HandlerType, val path: String, val ignoreTrailingSlashes: Boolean, val handler: Handler, val rawHandler: Handler) {
    private val pathParser = PathParser(path, ignoreTrailingSlashes)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
}

class PathMatcher() {

    private val handlerEntries = HandlerType.values().associateTo(EnumMap<HandlerType, ArrayList<HandlerEntry>>(HandlerType::class.java)) {
        it to arrayListOf()
    }

    fun add(entry: HandlerEntry) {
        if (entry.type.isHttpMethod() && handlerEntries[entry.type]!!.find { it.type == entry.type && it.path == entry.path } != null) {
            throw IllegalArgumentException("Handler with type='${entry.type}' and path='${entry.path}' already exists.")
        }
        handlerEntries[entry.type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String) =
            handlerEntries[handlerType]!!.filter { he -> match(he, requestUri) }

    private fun match(entry: HandlerEntry, requestPath: String): Boolean = when {
        entry.path == "*" -> true
        entry.path == requestPath -> true
        else -> entry.matches(requestPath)
    }

}
