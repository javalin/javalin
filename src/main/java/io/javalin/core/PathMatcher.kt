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

data class HandlerEntry(val type: HandlerType, val path: String, val handler: Handler)

class PathMatcher {

    var ignoreTrailingSlashes = true

    private val log = LoggerFactory.getLogger(PathMatcher::class.java)

    val handlerEntries = ArrayList<HandlerEntry>()

    fun findEntries(requestType: HandlerType, requestUri: String): List<HandlerEntry> {
        return handlerEntries.filter { he -> match(he, requestType, requestUri) }
    }

    // TODO: Consider optimizing this
    private fun match(handlerEntry: HandlerEntry, requestType: HandlerType, requestPath: String): Boolean = when {
        handlerEntry.type !== requestType -> false
        handlerEntry.path == "*" -> true
        handlerEntry.path == requestPath -> true
        !this.ignoreTrailingSlashes && slashMismatch(handlerEntry.path, requestPath) -> false
        else -> matchParamAndWildcard(handlerEntry.path, requestPath)
    }

    private fun slashMismatch(s1: String, s2: String): Boolean = (s1.endsWith('/') || s2.endsWith('/')) && (s1.last() != s2.last())

    private fun matchParamAndWildcard(handlerPath: String, fullRequestPath: String): Boolean {

        val hpp = Util.pathToList(handlerPath) // handler-path-parts
        val rpp = Util.pathToList(fullRequestPath) // request-path-parts

        fun isLastAndSplat(i: Int) = i == hpp.lastIndex && hpp[i] == "*"
        fun isNotPathOrSplat(i: Int) = hpp[i].first() != ':' && hpp[i] != "*"

        if (hpp.size == rpp.size) {
            for (i in hpp.indices) {
                when {
                    isLastAndSplat(i) && handlerPath.endsWith('*') -> return true
                    isNotPathOrSplat(i) && hpp[i] != rpp[i] -> return false
                }
            }
            return true
        }
        if (hpp.size < rpp.size && handlerPath.endsWith('*')) {
            for (i in hpp.indices) {
                when {
                    isLastAndSplat(i) -> return true
                    isNotPathOrSplat(i) && hpp[i] != rpp[i] -> return false
                }
            }
            return false
        }
        return false
    }

    fun findHandlerPath(predicate: (HandlerEntry) -> Boolean): String? {
        val entries = handlerEntries.filter(predicate)
        if (entries.size > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries[0].type, entries[0].path)
        }
        return if (entries.isNotEmpty()) entries[0].path else null
    }

}
