package io.javalin.core

import io.javalin.core.routing.ParameterNamesNotUniqueException
import io.javalin.core.routing.PathSegment
import io.javalin.core.routing.constructRegexList
import io.javalin.core.routing.convertSegment
import io.javalin.core.routing.pathParamNames
import io.javalin.core.routing.values
import io.javalin.http.util.ContextUtil

class PathParser(private val rawPath: String, ignoreTrailingSlashes: Boolean) {

    init {
        if (rawPath.contains("/:")) {
            throw IllegalArgumentException("Path '$rawPath' invalid - Javalin 4 switched from ':param' to '{param}'.")
        }
    }

    private val matchPathAndEverySubPath: Boolean = rawPath.endsWith(">*") || rawPath.endsWith("}*")
    private val path: String = if (matchPathAndEverySubPath) rawPath.removeSuffix("*") else rawPath

    val segments: List<PathSegment> = path.split("/")
        .filter { it.isNotEmpty() }
        .map { segment -> convertSegment(segment, rawPath) }

    val pathParamNames: List<String> = segments.map { it.pathParamNames() }.flatten().also { list ->
        val set = list.toSet()
        if (set.size != list.size) {
            throw ParameterNamesNotUniqueException(rawPath)
        }
    }

    //compute matchRegex suffix : if ignoreTrailingSlashes config is set we keep /?, else we use the true path trailing slash : present or absent
    private val regexSuffix = when {
        ignoreTrailingSlashes -> "/?"
        rawPath.endsWith("/") -> "/"
        else -> ""
    }

    private val matchRegex = constructRegexList(matchPathAndEverySubPath, segments, regexSuffix) { it.asRegexString() }
    private val pathParamRegex =
        constructRegexList(matchPathAndEverySubPath, segments, regexSuffix) { it.asGroupedRegexString() }

    fun matches(url: String): Boolean = matchRegex.any { url matches it }

    fun extractPathParams(url: String): Map<String, String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return pathParamNames.zip(values(pathParamRegex[index], url)) { name, value ->
            name to ContextUtil.urlDecode(value)
        }.toMap()
    }
}
