/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Handler
import io.javalin.core.util.ContextUtil.urlDecode
import org.slf4j.LoggerFactory
import java.util.EnumMap
import kotlin.collections.*

class PathParser(val path: String) {
    private val paramNames = path.split("/")
            .filter { it.startsWith(":") }
            .map { it.replace(":", "") }

    private val matchRegex = paramNames
            // Replace param names with wildcards (accepting everything except slash)
            .fold(path) { path, name -> path.replace(":$name", "[^/]+?") }
            // Replace double slash occurrences
            .replace("//", "/")
            // Replace splat between slashes to a wildcard
            .replace("/*/", "/.*?/")
            // Replace splat in the beginning of path to a wildcard (allow paths like (*/path/)
            .replace("^\\*".toRegex(), ".*?")
            // Replace splat in the end of string to a wildcard
            .replace("/*", "/.*?")
            // Replace trailing slash to optional one
            .replace("/$".toRegex(), "/?")
            // Add slash if doesn't have one
            .run { if (!endsWith("/?")) this + "/?" else this }
            // Let the matcher know that it is the whole path
            .run { "^" + this + "$" }
            .toRegex()

    // Use param wildcard as a capturing group
    private val paramRegex = matchRegex.pattern.replace("[^/]+?", "([^/]+?)").toRegex()

    // Use splat wildcard as a capturing group
    private val splatRegex = matchRegex.pattern.replace(".*?", "(.*?)").toRegex()

    fun matches(requestUri: String) = requestUri matches matchRegex

    fun extractParams(requestUri: String): Map<String, String> {
        val values = paramRegex.matchEntire(requestUri)?.groupValues
        val map = HashMap<String, String>()
        values?.let {
            (1 until values.size).forEach { index ->
                map[":" + paramNames[index - 1].toLowerCase()] = urlDecode(values[index])
            }
        }
        return map
    }

    fun extractSplats(requestUri: String): List<String> {
        val values = splatRegex.matchEntire(requestUri)?.groupValues
        val result = ArrayList<String>()
        values?.let {
            (1 until values.size).forEach { index ->
                result.add(urlDecode(values[index]))
            }
        }
        return result
    }

}

data class HandlerEntry(val type: HandlerType, val path: String, val handler: Handler) {
    private val parser: PathParser = PathParser(path)

    fun matches(requestUri: String) = parser.matches(requestUri)

    fun extractPathParams(requestUri: String): Map<String, String> = parser.extractParams(requestUri)

    fun extractSplats(requestUri: String): List<String> = parser.extractSplats(requestUri)
}

class PathMatcher {

    var ignoreTrailingSlashes = true

    private val log = LoggerFactory.getLogger(PathMatcher::class.java)

    val handlerEntries = HandlerType.values().associateTo(EnumMap<HandlerType, ArrayList<HandlerEntry>>(HandlerType::class.java)) {
        it to arrayListOf()
    }

    fun findEntries(requestType: HandlerType, requestUri: String): List<HandlerEntry> {
        return handlerEntries[requestType]!!.filter { he -> match(he, requestUri) }
    }

    private fun match(entry: HandlerEntry, requestPath: String): Boolean = when {
        entry.path == "*" -> true
        entry.path == requestPath -> true
        !this.ignoreTrailingSlashes && slashMismatch(entry.path, requestPath) -> false
        else -> entry.matches(requestPath)
    }

    private fun slashMismatch(s1: String, s2: String): Boolean = (s1.endsWith('/') || s2.endsWith('/')) && (s1.last() != s2.last())

    fun findHandlerPath(predicate: (HandlerEntry) -> Boolean): String? {
        val entries = handlerEntries.values.flatten().filter(predicate)
        if (entries.size > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries[0].type, entries[0].path)
        }
        return if (entries.isNotEmpty()) entries[0].path else null
    }

}
