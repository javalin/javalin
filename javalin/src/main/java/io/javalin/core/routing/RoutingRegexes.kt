package io.javalin.core.routing

internal fun constructRegexList(
    matchEverySubPath: Boolean,
    segments: List<PathSegment>,
    regexSuffix: String,
    regexOptions: Set<RegexOption> = emptySet(),
    mapper: (PathSegment) -> String
): List<Regex> {
    fun addRegexForExtraWildcard(): List<Regex> {
        return if (matchEverySubPath) {
            listOf(constructRegex(segments + PathSegment.Wildcard, regexSuffix, regexOptions, mapper))
        } else {
            emptyList()
        }
    }

    return listOf(constructRegex(segments, regexSuffix, regexOptions, mapper)) + addRegexForExtraWildcard()
}

internal fun constructRegex(
    segments: List<PathSegment>,
    regexSuffix: String,
    regexOptions: Set<RegexOption> = emptySet(),
    mapper: (PathSegment) -> String
): Regex {
    return buildString {
        append("^/")
        append(segments.joinToString(separator = "/", transform = mapper))
        append(regexSuffix)
        append("$")
    }.toRegex(regexOptions)
}

// Match and group values, then drop first element (the input string)
internal fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
