/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.routing

import io.javalin.config.RoutingConfig
import io.javalin.http.DefaultContext
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import java.util.*

data class HandlerEntry(
    val type: HandlerType,
    val path: String,
    val routingConfig: RoutingConfig,
    val roles: Set<RouteRole>,
    val handler: Handler,
) {
    private val pathParser = PathParser(path, routingConfig)
    fun matches(requestUri: String): Boolean = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String): Map<String, String> = pathParser.extractPathParams(requestUri)
    fun handle(ctx: DefaultContext, requestUri: String) = handler.handle(ctx.update(this, requestUri))
}

class PathMatcher {

    private val handlerEntries: Map<HandlerType, MutableList<HandlerEntry>> =
        HandlerType.values().associateWithTo(EnumMap(HandlerType::class.java)) { arrayListOf() }

    fun add(entry: HandlerEntry) {
        if (entry.type.isHttpMethod() && handlerEntries[entry.type]!!.find { it.type == entry.type && it.path == entry.path } != null) {
            throw IllegalArgumentException("Handler with type='${entry.type}' and path='${entry.path}' already exists.")
        }
        handlerEntries[entry.type]!!.add(entry)
    }

    fun findEntries(handlerType: HandlerType, requestUri: String) =
        handlerEntries[handlerType]!!.filter { he -> match(he, requestUri) }

    internal fun hasEntries(handlerType: HandlerType, requestUri: String): Boolean =
        handlerEntries[handlerType]!!.any { entry -> match(entry, requestUri) }

    private fun match(entry: HandlerEntry, requestPath: String): Boolean = when (entry.path) {
        "*" -> true
        requestPath -> true
        else -> entry.matches(requestPath)
    }

}
