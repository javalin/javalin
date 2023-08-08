package io.javalin.router.matcher

import io.javalin.config.RouterConfig
import io.javalin.http.servlet.urlDecode

class PathParser(private val rawPath: String, routerConfig: RouterConfig) {

    init {
        if (rawPath.contains("/:")) {
            throw IllegalArgumentException("Path '$rawPath' invalid - Javalin 4 switched from ':param' to '{param}'.")
        }
    }

    private val matchPathAndEverySubPath = rawPath.endsWith(">*") || rawPath.endsWith("}*")
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

    //compute matchRegex suffix :
    private val regexSuffix = if (routerConfig.treatMultipleSlashesAsSingleSlash) {
        // when multiple slashes are accepted we have to allow 0-n slashes when using ignoreTrailingSlashes
        // otherwise we also have to allow multiple slashes when only one slash is specified
        when {
            // the root path is special: we already add a leading slash during regex construction,
            // so we do not need to add an extra slash with the suffix
            rawPath == "/" -> ""
            routerConfig.ignoreTrailingSlashes -> "/*"
            rawPath.endsWith("/") -> "/+"
            else -> ""
        }
    } else {
        // if ignoreTrailingSlashes config is set we keep /?, else we use the true path trailing slash : present or absent
        when {
            // the root path is special: we already add a leading slash during regex construction,
            // so we do not need to add an extra slash with the suffix
            rawPath == "/" -> ""
            routerConfig.ignoreTrailingSlashes -> "/?"
            rawPath.endsWith("/") -> "/"
            else -> ""
        }
    }

    private val regexOptions: Set<RegexOption> = when {
        routerConfig.caseInsensitiveRoutes -> setOf(RegexOption.IGNORE_CASE)
        else -> emptySet()
    }

    private val matchRegex =
        constructRegexList(routerConfig, matchPathAndEverySubPath, segments, regexSuffix, regexOptions) { it.asRegexString() }
    private val pathParamRegex =
        constructRegexList(routerConfig, matchPathAndEverySubPath, segments, regexSuffix, regexOptions) { it.asGroupedRegexString() }

    fun matches(url: String): Boolean = matchRegex.any { url matches it }

    fun extractPathParams(url: String): Map<String, String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return pathParamNames.zip(values(pathParamRegex[index], url)) { name, value ->
            name to urlDecode(value)
        }.toMap()
    }
}
