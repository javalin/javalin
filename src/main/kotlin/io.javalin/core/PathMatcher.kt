/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Handler
import io.javalin.core.util.Util
import org.slf4j.LoggerFactory
import java.util.*

class PathMatcher {

    private val log = LoggerFactory.getLogger(PathMatcher::class.java)

    private val handlerEntries: MutableList<HandlerEntry> = ArrayList<HandlerEntry>()

    fun add(type: Handler.Type, path: String, handler: Handler) {
        handlerEntries.add(HandlerEntry(type, path, handler))
    }

    fun clear() {
        handlerEntries.clear()
    }

    fun findHandlers(type: Handler.Type, path: String): List<HandlerMatch> {
        return findTargetsForRequestedHandler(type, path).map { handlerEntry -> HandlerMatch(handlerEntry.handler, handlerEntry.path, path) }
    }

    private fun findTargetsForRequestedHandler(type: Handler.Type, path: String): List<HandlerEntry> {
        return handlerEntries.filter { he -> match(he, type, path) }
    }

    fun findHandlerPath(predicate: (HandlerEntry) -> Boolean): String? {
        val entries = handlerEntries.filter(predicate)
        if (entries.size > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries[0].type, entries[0].path)
        }
        return if (entries.isNotEmpty()) entries[0].path else null
    }

    // TODO: Consider optimizing this
    private fun match(handlerEntry: HandlerEntry, requestType: Handler.Type, requestPath: String): Boolean {
        if (handlerEntry.type !== requestType) {
            return false
        }
        if (endingSlashesDoNotMatch(handlerEntry.path, requestPath)) {
            return false
        }
        if (handlerEntry.path == requestPath) { // identical paths
            return true
        }
        return matchParamAndWildcard(handlerEntry.path, requestPath)
    }

    private fun matchParamAndWildcard(handlerPath: String, requestPath: String): Boolean {

        val handlerPaths = Util.pathToList(handlerPath)
        val requestPaths = Util.pathToList(requestPath)

        val numHandlerPaths = handlerPaths.size
        val numRequestPaths = requestPaths.size

        if (numHandlerPaths == numRequestPaths) {
            for (i in 0..numHandlerPaths - 1) {
                val handlerPathPart = handlerPaths[i]
                val requestPathPart = requestPaths[i]
                if (handlerPathPart == "*" && handlerPath.endsWith("*") && i == numHandlerPaths - 1) {
                    return true
                }
                if (handlerPathPart != "*" && !handlerPathPart.startsWith(":") && handlerPathPart != requestPathPart) {
                    return false
                }
            }
            return true
        }
        if (handlerPath.endsWith("*") && numHandlerPaths < numRequestPaths) {
            for (i in 0..numHandlerPaths - 1) {
                val handlerPathPart = handlerPaths[i]
                val requestPathPart = requestPaths[i]
                if (handlerPathPart == "*" && handlerPath.endsWith("*") && i == numHandlerPaths - 1) {
                    return true
                }
                if (!handlerPathPart.startsWith(":") && handlerPathPart != "*" && handlerPathPart != requestPathPart) {
                    return false
                }
            }
            return false
        }
        return false
    }

    private fun endingSlashesDoNotMatch(handlerPath: String, requestPath: String): Boolean {
        return requestPath.endsWith("/") && !handlerPath.endsWith("/") || !requestPath.endsWith("/") && handlerPath.endsWith("/")
    }

}
