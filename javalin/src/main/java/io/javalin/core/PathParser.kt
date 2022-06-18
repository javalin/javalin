package io.javalin.core

import io.javalin.core.routing.ParameterNamesNotUniqueException
import io.javalin.core.routing.PathSegment
import io.javalin.core.routing.constructRegexList
import io.javalin.core.routing.convertSegment
import io.javalin.core.routing.pathParamNames
import io.javalin.core.routing.values
import io.javalin.http.util.ContextUtil

data class PathParserOptions(val ignoreTrailingSlashes: Boolean, val treatMultipleSlashesAsSingleSlash: Boolean)

fun createPathParserOptionsFromConfig(config: JavalinConfig) = PathParserOptions(
    ignoreTrailingSlashes = config.ignoreTrailingSlashes,
    treatMultipleSlashesAsSingleSlash = config.treatMultipleSlashesAsSingleSlash
)

internal data class CombinedOptions constructor(
    val ignoreTrailingSlashes: Boolean = false,
    val treatMultipleSlashesAsSingleSlash: Boolean = false,
    val matchPathAndEverySubPath: Boolean = false
) {
    constructor(options: PathParserOptions) : this(
        ignoreTrailingSlashes = options.ignoreTrailingSlashes,
        treatMultipleSlashesAsSingleSlash = options.treatMultipleSlashesAsSingleSlash
    )
}

class PathParser(private val rawPath: String, options: PathParserOptions) {

    constructor(rawPath: String, config: JavalinConfig): this(rawPath, createPathParserOptionsFromConfig(config))

    init {
        if (rawPath.contains("/:")) {
            throw IllegalArgumentException("Path '$rawPath' invalid - Javalin 4 switched from ':param' to '{param}'.")
        }
    }

    private val combinedOptions: CombinedOptions = CombinedOptions(options).copy(
        matchPathAndEverySubPath = rawPath.endsWith(">*") || rawPath.endsWith("}*")
    )
    private val path: String = if (combinedOptions.matchPathAndEverySubPath) rawPath.removeSuffix("*") else rawPath

    val segments: List<PathSegment> = path.split("/")
        .filter { it.isNotEmpty() }
        .map { segment -> convertSegment(segment, rawPath) }

    val pathParamNames: List<String> = segments.map { it.pathParamNames() }.flatten().also { list ->
        val set = list.toSet()
        if (set.size != list.size) {
            throw ParameterNamesNotUniqueException(rawPath)
        }
    }

    //compute matchRegex suffix :
    private val regexSuffix = if(combinedOptions.treatMultipleSlashesAsSingleSlash) {
        // when multiple slashes are accepted we have to allow 0-n slashes when using ignoreTrailingSlashes
        // otherwise we also have to allow multiple slashes when only one slash is specified
        when {
            options.ignoreTrailingSlashes -> "/*"
            rawPath.endsWith("/") -> "/+"
            else -> ""
        }
    } else {
        // if ignoreTrailingSlashes config is set we keep /?, else we use the true path trailing slash : present or absent
        when {
            options.ignoreTrailingSlashes -> "/?"
            rawPath.endsWith("/") -> "/"
            else -> ""
        }
    }

    private val matchRegex = constructRegexList(combinedOptions, segments, regexSuffix) { it.asRegexString() }
    private val pathParamRegex =
        constructRegexList(combinedOptions, segments, regexSuffix) { it.asGroupedRegexString() }

    fun matches(url: String): Boolean = matchRegex.any { url matches it }

    fun extractPathParams(url: String): Map<String, String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return pathParamNames.zip(values(pathParamRegex[index], url)) { name, value ->
            name to ContextUtil.urlDecode(value)
        }.toMap()
    }
}
