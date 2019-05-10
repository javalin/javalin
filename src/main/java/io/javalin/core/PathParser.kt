/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.http.util.ContextUtil

class PathParser(path: String) {
    val parsedPath = ParsedPath.fromPath(path)

    fun matches(url: String): Boolean = url matches parsedPath.matchRegex

    fun extractPathParams(url: String) = parsedPath.pathParamNames
            .zip(values(parsedPath.pathParamRegex, url)) { name, value ->
                name to ContextUtil.urlDecode(value)
            }.toMap()

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
}

data class ParsedPath(val segments: List<PathSegment>) {
    companion object {
        fun fromPath(path: String): ParsedPath {
            val segments = path.split("/")
                    .filter { it.isNotEmpty() }
                    .map {
                        when {
                            it.startsWith(":") -> PathSegment.Parameter(it.removePrefix(":"))
                            it == "*" -> PathSegment.Wildcard
                            else -> PathSegment.Normal(it)
                        }
                    }
            return ParsedPath(segments)
        }
    }

    internal val matchRegex: Regex = "^/${segments.joinToString("/") { it.asRegexString() }}/?$".toRegex()

    internal val pathParamRegex: Regex = matchRegex.pattern
            .replace("[^/]+?", "([^/]+?)")
            .toRegex()

    internal val pathParamNames: List<String>
        get() = segments
                .filterIsInstance<PathSegment.Parameter>()
                .map { it.name }


}

sealed class PathSegment {
    class Normal(val content: String) : PathSegment() {
        override fun asRegexString(): String = content
    }

    class Parameter(val name: String) : PathSegment() {
        override fun asRegexString(): String = "[^/]+?" // Accepting everything except slash
    }

    object Wildcard : PathSegment() {
        override fun asRegexString(): String = ".*?" // Accept everything
    }

    internal abstract fun asRegexString(): String
}
