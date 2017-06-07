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

    fun add(type: HandlerType, path: String, handler: Handler) = handlerEntries.add(HandlerEntry(type, path, handler))

    fun clear() = handlerEntries.clear()

    fun findEntries(requestType: HandlerType, requestUri: String): List<HandlerEntry> {
        return handlerEntries.filter { he -> match(he, requestType, requestUri) }
    }

    // TODO: Consider optimizing this
    private fun match(handlerEntry: HandlerEntry, requestType: HandlerType, requestPath: String): Boolean {
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

    private fun matchParamAndWildcard(fullHandlerPath: String, fullRequestPath: String): Boolean {

        val handlerPathParts = Util.pathToList(fullHandlerPath)
        val requestPathParts = Util.pathToList(fullRequestPath)

        val numHandlerPaths = handlerPathParts.size
        val numRequestPaths = requestPathParts.size

        if (numHandlerPaths == numRequestPaths) {
            handlerPathParts.forEachIndexed({ i, handlerPart ->
                val requestPart = requestPathParts[i]
                if (handlerPart == "*" && fullHandlerPath.last() == '*' && i == numHandlerPaths - 1) {
                    return true
                }
                if (handlerPart != "*" && handlerPart.first() != ':' && handlerPart != requestPart) {
                    return false
                }
            })
            return true
        }
        if (fullHandlerPath.last() == '*' && numHandlerPaths < numRequestPaths) {
            handlerPathParts.forEachIndexed({ i, handlerPart ->
                val requestPart = requestPathParts[i]
                if (handlerPart == "*" && fullHandlerPath.last() == '*' && i == numHandlerPaths - 1) {
                    return true
                }
                if (handlerPart != "*" && handlerPart.first() != ':' && handlerPart != requestPart) {
                    return false
                }
            })
            return false
        }
        return false
    }

    private fun endingSlashesDoNotMatch(handlerPath: String, requestPath: String): Boolean =
            (handlerPath.last() == '/' || requestPath.last() == '/') && (handlerPath.last() != requestPath.last())

    fun findHandlerPath(predicate: (HandlerEntry) -> Boolean): String? {
        val entries = handlerEntries.filter(predicate)
        if (entries.size > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries[0].type, entries[0].path)
        }
        return if (entries.isNotEmpty()) entries[0].path else null
    }

}
