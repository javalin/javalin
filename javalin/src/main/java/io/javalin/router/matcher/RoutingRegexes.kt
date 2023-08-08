package io.javalin.router.matcher

import io.javalin.config.RouterConfig
import io.javalin.router.matcher.PathSegment.Wildcard


internal fun constructRegexList(
    options: RouterConfig,
    matchEverySubPath: Boolean,
    segments: List<PathSegment>,
    regexSuffix: String,
    regexOptions: Set<RegexOption> = emptySet(),
    mapper: (PathSegment) -> String
): List<Regex> {
    fun addRegexForExtraWildcard(): List<Regex> {
        return if (matchEverySubPath) {
            listOf(constructRegex(options, segments + Wildcard, regexSuffix, regexOptions, mapper))
        } else {
            emptyList()
        }
    }

    return listOf(constructRegex(options, segments, regexSuffix, regexOptions, mapper)) + addRegexForExtraWildcard()
}

internal fun constructRegex(
    options: RouterConfig,
    segments: List<PathSegment>,
    regexSuffix: String,
    regexOptions: Set<RegexOption> = emptySet(),
    mapper: (PathSegment) -> String
): Regex {
    val slashRegex = if (options.treatMultipleSlashesAsSingleSlash) {
        "/+"
    } else {
        "/"
    }
    return buildString {
        append("^/")
        if (options.treatMultipleSlashesAsSingleSlash) {
            append("+")
        }
        append(segments.joinToString(separator = slashRegex, transform = mapper))
        append(regexSuffix)
        append("$")
    }.toRegex(regexOptions)
}

// Match and group values, then drop first element (the input string)
internal fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
