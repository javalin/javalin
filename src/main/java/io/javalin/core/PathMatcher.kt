/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Handler
import io.javalin.core.util.ContextUtil.urlDecode
import java.util.*

data class HandlerEntry(val type: HandlerType, val path: String, val handler: Handler, val rawHandler: Handler, val caseSensitiveUrls: Boolean) {
    private val pathParser = PathParser(path, caseSensitiveUrls)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
    fun extractSplats(requestUri: String) = pathParser.extractSplats(requestUri)
}

class PathParser(
        path: String,
        private val caseSensitive: Boolean,
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
                .toCasedRegex(caseSensitive),
        private val splatRegex: Regex = matchRegex.pattern.replace(".*?", "(.*?)").toRegex(RegexOption.IGNORE_CASE),
        private val pathParamRegex: Regex = matchRegex.pattern.replace("[^/]+?", "([^/]+?)").toRegex(RegexOption.IGNORE_CASE)) {

    fun matches(url: String) = url matches matchRegex

    fun extractPathParams(url: String) = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name to urlDecode(value)
    }.toMap()

    fun extractSplats(url: String) = values(splatRegex, url).map { urlDecode(it) }

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()

}

private fun String.toCasedRegex(caseSensitive: Boolean) = this.let { if (caseSensitive) it.toRegex() else it.toRegex(RegexOption.IGNORE_CASE) }

class PathMatcher(var ignoreTrailingSlashes: Boolean = true) {

    private val handlerEntries = HandlerType.values().associateTo(EnumMap<HandlerType, ArrayList<HandlerEntry>>(HandlerType::class.java)) {
        it to arrayListOf()
    }

    fun add(entry: HandlerEntry) {
        if (!entry.caseSensitiveUrls && entry.path != entry.path.toLowerCase()) {
            throw IllegalArgumentException("By default URLs must be lowercase. Change casing or call `app.enableCaseSensitiveUrls()` to allow mixed casing.")
        }
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
        !this.ignoreTrailingSlashes && slashMismatch(entry.path, requestPath) -> false
        else -> entry.matches(requestPath)
    }

    private fun slashMismatch(s1: String, s2: String) = (s1.endsWith('/') || s2.endsWith('/')) && (s1.last() != s2.last())

}
