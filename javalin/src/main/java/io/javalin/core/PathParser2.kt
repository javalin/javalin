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

class PathParser2(private val path: String, private val options: PathParserOptions) {

    private val adjacentViolations: List<String> = listOf(
            "*${options.openingDelimiterType1}",
            "${options.closingDelimiterType1}*",
            "*${options.openingDelimiterType2}",
            "${options.closingDelimiterType2}*"
    )

    private val allDelimiters: Set<Char> = setOf(
            options.openingDelimiterType1,
            options.openingDelimiterType2,
            options.closingDelimiterType1,
            options.closingDelimiterType2
    )

    internal val segments: List<PathSegment2> = path.split("/")
            .filter { it.isNotEmpty() }
            .map(::convertSegment)

    private fun convertSegment(segment: String): PathSegment2 {
        val brackets = segment.count { it in allDelimiters }
        val wildcards = segment.count { it == '*' }
        // avoid parsing malformed paths
        if (brackets % 2 != 0 && !options.allowOptionalClosingDelimiter) {
            throw MissingBracketsException(segment, path)
        }
        if (adjacentViolations.any { it in segment }) {
            throw WildcardBracketAdjacentException(segment, path)
        }

        // early return for the most common scenarios
        when {
            // just a Parameter in this segment
            // checking the number of brackets enforces a path parameter name without those brackets
            brackets == 2 && segment.startsWith(options.openingDelimiterType1) && segment.endsWith(options.closingDelimiterType1) -> return PathSegment2.Parameter.SlashIgnoringParameter(segment.removePrefix(options.openingDelimiterType1.toString()).removeSuffix(options.closingDelimiterType1.toString()))
            brackets == 2 && segment.startsWith(options.openingDelimiterType2) && segment.endsWith(options.closingDelimiterType2) -> return PathSegment2.Parameter.SlashAcceptingParameter(segment.removePrefix(options.openingDelimiterType2.toString()).removeSuffix(options.closingDelimiterType2.toString()))
            brackets == 1 && segment.startsWith(options.openingDelimiterType1) -> return PathSegment2.Parameter.SlashIgnoringParameter(segment.removePrefix(options.openingDelimiterType1.toString()))
            // just a wildcard
            segment == "*" -> return PathSegment2.Wildcard
            // no special characters
            brackets == 0 && wildcards == 0 -> return PathSegment2.Normal(segment)
        }

        return parseAsPathSegment(segment)
    }

    private fun parseAsPathSegment(segment: String): PathSegment2 {
        var state: ParserState = ParserState.NORMAL
        val pathNameAccumulator = mutableListOf<Char>()
        fun mapSingleChar(char: Char): PathSegment2? {
            when (state) {
                ParserState.NORMAL -> {
                    return when (char) {
                        '*' -> PathSegment2.Wildcard
                        options.openingDelimiterType1 -> {
                            state = ParserState.INSIDE_BRACKET_TYPE_1
                            null
                        }
                        options.openingDelimiterType2 -> {
                            state = ParserState.INSIDE_BRACKET_TYPE_2
                            null
                        }
                        options.closingDelimiterType1, options.closingDelimiterType2 -> throw MissingBracketsException(segment, path) // cannot start with a closing delimiter
                        else -> PathSegment2.Normal(char.toString()) // the single characters will be merged later
                    }
                }
                ParserState.INSIDE_BRACKET_TYPE_1 -> {
                    return when (char) {
                        options.closingDelimiterType1 -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment2.Parameter.SlashIgnoringParameter(name)
                        }
                        options.openingDelimiterType1, options.openingDelimiterType2, options.closingDelimiterType2 -> throw MissingBracketsException(segment, path) // cannot start another variable inside a variable
                        // wildcard is also okay inside a variable name
                        else -> {
                            pathNameAccumulator += char
                            null
                        }
                    }
                }
                ParserState.INSIDE_BRACKET_TYPE_2 -> {
                    return when (char) {
                        options.closingDelimiterType2 -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment2.Parameter.SlashAcceptingParameter(name)
                        }
                        options.openingDelimiterType1, options.openingDelimiterType2, options.closingDelimiterType1 -> throw MissingBracketsException(segment, path) // cannot start another variable inside a variable
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
                .fold(PathSegment2.MultipleSegments(emptyList())) { acc, pathSegment ->
                    val lastAddition = acc.innerSegments.lastOrNull()
                    when {
                        lastAddition == null -> PathSegment2.MultipleSegments(listOf(pathSegment))
                        lastAddition is PathSegment2.Wildcard && pathSegment is PathSegment2.Wildcard -> acc
                        lastAddition is PathSegment2.Normal && pathSegment is PathSegment2.Normal -> PathSegment2.MultipleSegments(acc.innerSegments.dropLast(1) + PathSegment2.Normal(lastAddition.content + pathSegment.content))
                        else -> PathSegment2.MultipleSegments(acc.innerSegments + pathSegment)
                    }
                }
    }

    internal val pathParamNames: List<String> = segments.map { it.pathParamNames() }.flatten()

    private val matchRegex = "^/${segments.joinToString("/") { it.asRegexString() }}/?$".toRegex()

    private val pathParamRegex = "^/${segments.joinToString("/") { it.asGroupedRegexString() }}/?$".toRegex()

    fun matches(url: String): Boolean = url matches matchRegex

    fun extractPathParams(url: String): Map<String, String> = pathParamNames.zip(values(pathParamRegex, url)) { name, value ->
        name to ContextUtil.urlDecode(value)
    }.toMap()

    // Match and group values, then drop first element (the input string)
    private fun values(regex: Regex, url: String) = regex.matchEntire(url)?.groupValues?.drop(1) ?: emptyList()
}

sealed class PathSegment2 {

    internal abstract fun asRegexString(): String

    internal open fun asGroupedRegexString(): String = asRegexString()

    class Normal(val content: String) : PathSegment2() {
        override fun asRegexString(): String = content
    }

    sealed class Parameter(val name: String) : PathSegment2() {
        class SlashIgnoringParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = "[^/]+?" // Accept everything except slash
            override fun asGroupedRegexString(): String = "(${asRegexString()})"
        }

        class SlashAcceptingParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = ".+?" // Accept everything
            override fun asGroupedRegexString(): String = "(${asRegexString()})"
        }
    }

    object Wildcard : PathSegment2() {
        override fun asRegexString(): String = ".*?" // Accept everything
    }

    class MultipleSegments(segments: List<PathSegment2>) : PathSegment2() {
        init {
            if (segments.filterIsInstance<MultipleSegments>().isNotEmpty()) {
                throw IllegalStateException("Found MultipleSegment inside MultipleSegments! This is forbidden")
            }
        }

        val innerSegments = segments.filterNot { it is MultipleSegments }

        private val regex: String = innerSegments.joinToString(separator = "") { it.asRegexString() }
        private val groupedRegex: String = innerSegments.joinToString(separator = "") { it.asGroupedRegexString() }
        override fun asRegexString(): String = regex
        override fun asGroupedRegexString(): String = groupedRegex
    }
}

fun List<PathSegment2>.flattenMultipleSegments(): List<PathSegment2> {
    return this.map {
        if (it is PathSegment2.MultipleSegments) {
            it.innerSegments
        } else {
            listOf(it)
        }
    }.flatten()
}

private fun PathSegment2.pathParamNames(): List<String> {
    return when (this) {
        is PathSegment2.Normal, is PathSegment2.Wildcard -> emptyList()
        is PathSegment2.Parameter -> listOf(this.name)
        is PathSegment2.MultipleSegments -> this.innerSegments.filterIsInstance<PathSegment2.Parameter>().map { it.name }
    }
}
