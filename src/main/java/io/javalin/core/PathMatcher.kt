/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Handler
import io.javalin.core.util.ContextUtil.urlDecode
import org.slf4j.LoggerFactory
import java.util.*

data class HandlerEntry(val type: HandlerType, val path: String, val handler: Handler, val rawHandler: Handler, private val lowerCasePaths:Boolean ) {
    private val pathParser = PathParser(path,lowerCasePaths)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
    fun extractSplats(requestUri: String) = pathParser.extractSplats(requestUri)
}

class PathParser(
        path: String,
        private val lowerCasePaths:Boolean,
        private val pathParamNames: List<String> = path.split("/")
                .filter { it.startsWith(":") }
                .map { it.replace(":", "") },
        private val matchRegex: Regex = pathParamNames
                .fold(path) { p, name -> p.replace(":$name", "[^/]+?") } // Replace path param names with wildcards (accepting everything except slash)
                .replace("//", "/") // Replace double slash occurrences
                .replace("/*/", "/.*?/") // Replace splat between slashes to a wildcard
                .replace("^\\*".toRegex(), ".*?") // Replace splat in the beginning of path to a wildcard (allow paths like (*/path/)
                .replace("/*", "/.*?") // Replace splat in the end of string to a wildcard
                .replace("/$".toRegex(), "/?") // Replace trailing slash to optional one
                .run { if (!endsWith("/?")) this + "/?" else this } // Add slash if doesn't have one
                .run { "^" + this + "$" } // Let the matcher know that it is the whole path
                .toRegex(),
        private val splatRegex: Regex = matchRegex.pattern.replace(".*?", "(.*?)").toRegex(),
        private val pathParamRegex: Regex = matchRegex.pattern.replace("[^/]+?", "([^/]+?)").toRegex()) {

    fun matches(url: String) = url matches matchRegex

    fun extractPathParams(url: String) = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name.run { if ( lowerCasePaths ) toLowerCase() else this } to urlDecode(value)
    }.toMap()

    fun extractSplats(url: String) = values(splatRegex, url).map { urlDecode(it) }

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()

}

class PathMatcher(var ignoreTrailingSlashes: Boolean = true) {

    private val log = LoggerFactory.getLogger(PathMatcher::class.java)

    val handlerEntries = HandlerType.values().associateTo(EnumMap<HandlerType, ArrayList<HandlerEntry>>(HandlerType::class.java)) {
        it to arrayListOf()
    }

    fun findEntries(requestType: HandlerType, requestUri: String) =
            handlerEntries[requestType]!!.filter { he -> match(he, requestUri) }

    private fun match(entry: HandlerEntry, requestPath: String): Boolean = when {
        entry.path == "*" -> true
        entry.path == requestPath -> true
        !this.ignoreTrailingSlashes && slashMismatch(entry.path, requestPath) -> false
        else -> entry.matches(requestPath)
    }

    private fun slashMismatch(s1: String, s2: String) = (s1.endsWith('/') || s2.endsWith('/')) && (s1.last() != s2.last())

    fun findHandlerPath(predicate: (HandlerEntry) -> Boolean): String? {
        val entries = handlerEntries.values.flatten().filter(predicate)
        if (entries.size > 1) {
            log.warn("More than one path found for handler, returning first match: '{} {}'", entries[0].type, entries[0].path)
        }
        return if (entries.isNotEmpty()) entries[0].path else null
    }

}
