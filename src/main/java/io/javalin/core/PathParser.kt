/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.http.util.ContextUtil

class PathParser(
        path: String,
        private val pathParamNames: List<String> = path.split("/")
                .filter { it.startsWith(":") }
                .map { it.replace(":", "") },
        private val matchRegex: Regex = pathParamNames
                .fold(path) { p, name -> p.replace(":$name", "[^/]+?") } // Replace path param names with wildcards (accepting everything except slash)
                .replace("//", "/") // Replace double slash occurrences
                .replace("/*/", "/.*?/") // Replace star between slashes with wildcard
                .replace("^\\*".toRegex(), ".*?") // Replace star in the beginning of path with wildcard (allow paths like (*/path/)
                .replace("/*", "/.*?") // Replace star in the end of string with wildcard
                .replace("/$".toRegex(), "/?") // Replace trailing slash with optional one
                .run { if (!endsWith("/?")) this + "/?" else this } // Add slash if doesn't have one
                .run { "^" + this + "$" } // Let the matcher know that it is the whole path
                .toRegex(),
        private val pathParamRegex: Regex = matchRegex.pattern.replace("[^/]+?", "([^/]+?)").toRegex(RegexOption.IGNORE_CASE)) {

    fun matches(url: String) = url matches matchRegex

    fun extractPathParams(url: String) = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name to ContextUtil.urlDecode(value)
    }.toMap()

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()

}
