/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.http.util.ContextUtil

class PathParser(path: String) {

    internal val segments: List<PathSegment> = path.split("/")
            .filter { it.isNotEmpty() }
            .map {
                when {
                    it.startsWith(":") -> PathSegment.Parameter(it.removePrefix(":"))
                    it.startsWith("{") -> PathSegment.Parameter(
                            it.removePrefix("{").split(":").first(),
                            it.removeSuffix("}").split(":").drop(1).joinToString(":")
                    )
                    it == "*" -> PathSegment.Wildcard
                    else -> PathSegment.Normal(it)
                }
            }

    internal val pathParamNames = segments.filterIsInstance<PathSegment.Parameter>().map { it.name }

    private val matchRegex = "^/${segments.joinToString("/") { it.asRegexString() }}/?$".toRegex()

    private val pathParamRegex = "^/${segments.joinToString("/") { it.asRegexString(true) }}/?$".toRegex()

    fun matches(url: String): Boolean = url matches matchRegex

    fun extractPathParams(url: String) = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name to ContextUtil.urlDecode(value)
    }.toMap()

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
}

sealed class PathSegment {
    /**
     * @param extract surround the regex with brackets if it is a path parameter.
     */
    internal abstract fun asRegexString(extract: Boolean = false): String

    /**
     * @param content the content (literal between two dashes of an url) this segment represents
     */
    class Normal(val content: String) : PathSegment() {
        override fun asRegexString(extract: Boolean): String = content
    }

    /**
     * @param name the name of the parameter this segment represents
     * @param regex the regex to match for the path detection, the default is accept everything except slash
     */
    class Parameter(val name: String, val regex: String = "[^/]+?") : PathSegment() {
        override fun asRegexString(extract: Boolean): String = if (extract) "($regex)" else regex
    }

    /**
     * A segment representing a wildcard, matching everything including dashes.
     */
    object Wildcard : PathSegment() {
        override fun asRegexString(extract: Boolean): String = ".*?" // Accept everything
    }
}
