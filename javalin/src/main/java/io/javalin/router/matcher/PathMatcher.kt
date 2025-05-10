/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.matcher

import io.javalin.http.HandlerType
import io.javalin.router.ParsedEndpoint
import io.javalin.util.JavalinLogger
import java.util.*
import java.util.stream.Stream

class PathMatcher {

    private val handlerEntries: Map<HandlerType, MutableList<ParsedEndpoint>> =
        HandlerType.entries.associateWithTo(EnumMap(HandlerType::class.java)) { arrayListOf() }

    fun add(entry: ParsedEndpoint) {
        JavalinLogger.info("PathMatcher.add")
    
        val type = entry.endpoint.method
        val path = entry.endpoint.path

        if (type.isHttpMethod && handlerEntries[type]!!.find { it.endpoint.method == type && it.endpoint.path == path } != null) {
            throw IllegalArgumentException("Handler with type='${type}' and path='${path}' already exists.")
        }

        handlerEntries[type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String?): Stream<ParsedEndpoint> {
        JavalinLogger.info("PathMatcher.findEntries(handlerType : ${handlerType}, uri : ${requestUri})")
        // all gets hander endpoints
        // JavalinLogger.info("handerEntries[${handlerType}] = ${handlerEntries[handlerType]}")
        return when (requestUri) {
            null -> handlerEntries[handlerType]!!.stream()
            else -> handlerEntries[handlerType]!!.stream().filter { he -> match(he, requestUri) }
        }
    }

    fun allEntries() = handlerEntries.values.flatten()

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean {
        JavalinLogger.info("PathMatcher.hasEntries(handlerType : ${handlerType}, requestUri: ${requestUri}")
        return handlerEntries[handlerType]!!.any { entry -> match(entry, requestUri) }
    }

    private fun match(entry: ParsedEndpoint, requestPath: String): Boolean {
        
        var res =  when (entry.endpoint.path) {
            "*" -> true
            requestPath -> true
            else -> entry.matches(requestPath)
        }
        JavalinLogger.info(
            "PathMatcher.match([${entry.endpoint.method}, ${entry.endpoint.path}],"
            + " ${requestPath}) = ${res})")
        return res
    }

}
