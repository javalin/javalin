/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.http.util.ContextUtil

class PathParser(path: String, ignoreTrailingSlashes: Boolean) {

    val segments: List<PathSegment> = path.split("/")
            .filter { !ignoreTrailingSlashes || it.isNotEmpty() } //if not ignoreTrailingSlashes process every token, else only process not empty token
            .map {
                when {
                    it.startsWith(":") -> PathSegment.Parameter(it.removePrefix(":"))
                    it == "*" -> PathSegment.Wildcard
                    else -> PathSegment.Normal(it)
                }
            }

    val pathParamNames = segments.filterIsInstance<PathSegment.Parameter>().map { it.name }

    private val matchRegex = "^/${segments.joinToString("/") { it.asRegexString() }}/?$".toRegex()

    private val pathParamRegex = matchRegex.pattern.replace("[^/]+?", "([^/]+?)").toRegex()

    fun matches(url: String): Boolean = url matches matchRegex

    fun extractPathParams(url: String) = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name to ContextUtil.urlDecode(value)
    }.toMap()

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
}

sealed class PathSegment {

    internal abstract fun asRegexString(): String

    class Normal(val content: String) : PathSegment() {
        override fun asRegexString(): String = content
    }

    class Parameter(val name: String) : PathSegment() {
        override fun asRegexString(): String = "[^/]+?" // Accepting everything except slash
    }

    object Wildcard : PathSegment() {
        override fun asRegexString(): String = ".*?" // Accept everything
    }
}
