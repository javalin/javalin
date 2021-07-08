package io.javalin.core

import io.javalin.http.util.ContextUtil

class MissingBracketsException(val segment: String, val path: String) : RuntimeException(
        "This segment '$segment' is missing some brackets! Found in path '$path'"
)

class WildcardBracketAdjacentException(val segment: String, val path: String) : RuntimeException(
        "Wildcard and a path parameter bracket are adjacent in segment '$segment' of path '$path'. This is forbidden"
)

private enum class ParserState {
    NORMAL,
    INSIDE_BRACKET_TYPE_1,
    INSIDE_BRACKET_TYPE_2
}

data class PathParserOptions(
        val openingDelimiterType1: Char = '{',
        val closingDelimiterType1: Char = '}',
        val allowOptionalClosingDelimiter: Boolean = false,
        val openingDelimiterType2: Char = '<',
        val closingDelimiterType2: Char = '>'
)

class PathParser(private val rawPath: String, ignoreTrailingSlashes: Boolean) {

    private val adjacentViolations: List<String> = listOf(
            "*{",
            "*<",
            ">*"
    )

    private val allDelimiters: Set<Char> = setOf(
            '{',
            '}',
            '<',
            '>'
    )

    private val matchEverySubPath: Boolean = rawPath.endsWith("**")
    private val path: String = rawPath.removeSuffix("**")

    internal val segments: List<PathSegment> = path.split("/")
            .filter { it.isNotEmpty() }
            .map(::convertSegment)

    private fun convertSegment(segment: String): PathSegment {
        val brackets = segment.count { it in allDelimiters }
        val wildcards = segment.count { it == '*' }
        // avoid parsing malformed paths
        if (brackets % 2 != 0) {
            throw MissingBracketsException(segment, rawPath)
        }
        if (adjacentViolations.any { it in segment }) {
            throw WildcardBracketAdjacentException(segment, rawPath)
        }

        // early return for the most common scenarios
        when {
            // just a Parameter in this segment
            // checking the number of brackets enforces a path parameter name without those brackets
            brackets == 2 && segment.startsWith('{') && segment.endsWith('}') -> return PathSegment.Parameter.SlashIgnoringParameter(segment.removePrefix("{").removeSuffix("}"))
            brackets == 2 && segment.startsWith('<') && segment.endsWith('>') -> return PathSegment.Parameter.SlashAcceptingParameter(segment.removePrefix("<").removeSuffix(">"))
            // just a wildcard
            segment == "*" -> return PathSegment.Wildcard
            // no special characters
            brackets == 0 && wildcards == 0 -> return PathSegment.Normal(segment)
        }

        return parseAsPathSegment(segment)
    }

    private fun parseAsPathSegment(segment: String): PathSegment {
        var state: ParserState = ParserState.NORMAL
        val pathNameAccumulator = mutableListOf<Char>()
        fun mapSingleChar(char: Char): PathSegment? {
            when (state) {
                ParserState.NORMAL -> {
                    return when (char) {
                        '*' -> PathSegment.Wildcard
                        '{' -> {
                            state = ParserState.INSIDE_BRACKET_TYPE_1
                            null
                        }
                        '<' -> {
                            state = ParserState.INSIDE_BRACKET_TYPE_2
                            null
                        }
                        '}', '>' -> throw MissingBracketsException(segment, rawPath) // cannot start with a closing delimiter
                        else -> PathSegment.Normal(char.toString()) // the single characters will be merged later
                    }
                }
                ParserState.INSIDE_BRACKET_TYPE_1 -> {
                    return when (char) {
                        '}' -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment.Parameter.SlashIgnoringParameter(name)
                        }
                        '{', '<', '>' -> throw MissingBracketsException(segment, rawPath) // cannot start another variable inside a variable
                        // wildcard is also okay inside a variable name
                        else -> {
                            pathNameAccumulator += char
                            null
                        }
                    }
                }
                ParserState.INSIDE_BRACKET_TYPE_2 -> {
                    return when (char) {
                        '>' -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment.Parameter.SlashAcceptingParameter(name)
                        }
                        '{', '}', '<' -> throw MissingBracketsException(segment, rawPath) // cannot start another variable inside a variable
                        // wildcard is also okay inside a variable name
                        else -> {
                            pathNameAccumulator += char
                            null
                        }
                    }
                }
            }
        }

        return segment.map(::mapSingleChar)
                .filterNotNull()
                .fold(PathSegment.MultipleSegments(emptyList())) { acc, pathSegment ->
                    val lastAddition = acc.innerSegments.lastOrNull()
                    when {
                        lastAddition == null -> PathSegment.MultipleSegments(listOf(pathSegment))
                        lastAddition is PathSegment.Wildcard && pathSegment is PathSegment.Wildcard -> acc
                        lastAddition is PathSegment.Normal && pathSegment is PathSegment.Normal -> PathSegment.MultipleSegments(acc.innerSegments.dropLast(1) + PathSegment.Normal(lastAddition.content + pathSegment.content))
                        else -> PathSegment.MultipleSegments(acc.innerSegments + pathSegment)
                    }
                }
    }

    internal val pathParamNames: List<String> = segments.map { it.pathParamNames() }.flatten().also {
        list -> run {
            val set = list.toSet()
            if (set.size != list.size) {
                throw IllegalArgumentException("duplicate path param names detected!")
            }
        }
    }

    //compute matchRegex suffix : if ignoreTrailingSlashes config is set we keep /?, else we use the true path trailing slash : present or absent
    private val regexSuffix = when {
        ignoreTrailingSlashes -> "/?"
        rawPath.endsWith("/") -> "/"
        else -> ""
    }

    private val matchRegex = constructRegexList(matchEverySubPath, segments, regexSuffix) { it.asRegexString() }
    private val pathParamRegex = constructRegexList(matchEverySubPath, segments, regexSuffix) { it.asGroupedRegexString() }
    private val splatRegex = constructRegexList(matchEverySubPath, segments, regexSuffix, setOf(RegexOption.IGNORE_CASE)) {it.asSplatRegexString()}

    fun matches(url: String): Boolean = matchRegex.any { url matches it }

    fun extractPathParams(url: String): Map<String, String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return pathParamNames.zip(values(pathParamRegex[index], url)) { name, value ->
            name to ContextUtil.urlDecode(value)
        }.toMap()
    }

    fun extractSplats(url: String): List<String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return values(splatRegex[index], url).map { ContextUtil.urlDecode(it) }
    }

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
}

private fun constructRegexList(
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

private fun constructRegex(
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

private fun String.grouped() = "($this)"

sealed class PathSegment {

    internal abstract fun asRegexString(): String

    internal abstract fun asGroupedRegexString(): String

    internal abstract fun asSplatRegexString(): String

    class Normal(val content: String) : PathSegment() {
        // do not group static content
        override fun asRegexString(): String = content
        override fun asGroupedRegexString(): String = content
        override fun asSplatRegexString(): String = content
    }

    sealed class Parameter(val name: String) : PathSegment() {
        class SlashIgnoringParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = "[^/]+?" // Accept everything except slash
            override fun asGroupedRegexString(): String = asRegexString().grouped()
            override fun asSplatRegexString(): String = asRegexString()
        }

        class SlashAcceptingParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = ".+?" // Accept everything
            override fun asGroupedRegexString(): String = asRegexString().grouped()
            override fun asSplatRegexString(): String = asRegexString()
        }
    }

    object Wildcard : PathSegment() {
        override fun asRegexString(): String = ".*?" // Accept everything
        override fun asGroupedRegexString(): String = asRegexString()
        override fun asSplatRegexString(): String = asRegexString().grouped()
    }

    class MultipleSegments(segments: List<PathSegment>) : PathSegment() {
        init {
            if (segments.filterIsInstance<MultipleSegments>().isNotEmpty()) {
                throw IllegalStateException("Found MultipleSegment inside MultipleSegments! This is forbidden")
            }
        }

        val innerSegments = segments.filterNot { it is MultipleSegments }

        private val regex: String = innerSegments.joinToString(separator = "") { it.asRegexString() }
        private val groupedRegex: String = innerSegments.joinToString(separator = "") { it.asGroupedRegexString() }
        private val splatRegex: String = innerSegments.joinToString(separator = "") { it.asSplatRegexString() }
        override fun asRegexString(): String = regex
        override fun asGroupedRegexString(): String = groupedRegex
        override fun asSplatRegexString(): String = splatRegex
    }
}

fun List<PathSegment>.flattenMultipleSegments(): List<PathSegment> {
    return this.map {
        if (it is PathSegment.MultipleSegments) {
            it.innerSegments
        } else {
            listOf(it)
        }
    }.flatten()
}

private fun PathSegment.pathParamNames(): List<String> {
    return when (this) {
        is PathSegment.Normal, is PathSegment.Wildcard -> emptyList()
        is PathSegment.Parameter -> listOf(this.name)
        is PathSegment.MultipleSegments -> this.innerSegments.filterIsInstance<PathSegment.Parameter>().map { it.name }
    }
}
