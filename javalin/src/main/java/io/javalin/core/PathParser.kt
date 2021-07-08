package io.javalin.core

import io.javalin.core.routing.*
import io.javalin.core.routing.constructRegexList
import io.javalin.http.util.ContextUtil

private enum class ParserState {
    NORMAL,
    INSIDE_CURLY_BRACKETS,
    INSIDE_ANGLE_BRACKETS
}

class PathParser(private val rawPath: String, ignoreTrailingSlashes: Boolean) {

    private val adjacentViolations: List<String> = listOf(
        "*{",
        "*<",
        "}*",
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

    val segments: List<PathSegment> = path.split("/")
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
            brackets == 2 && segment.startsWith('{') && segment.endsWith('}') -> return PathSegment.Parameter.SlashIgnoringParameter(
                segment.removePrefix("{").removeSuffix("}")
            )
            brackets == 2 && segment.startsWith('<') && segment.endsWith('>') -> return PathSegment.Parameter.SlashAcceptingParameter(
                segment.removePrefix("<").removeSuffix(">")
            )
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
                            state = ParserState.INSIDE_CURLY_BRACKETS
                            null
                        }
                        '<' -> {
                            state = ParserState.INSIDE_ANGLE_BRACKETS
                            null
                        }
                        '}', '>' -> throw MissingBracketsException(
                            segment,
                            rawPath
                        ) // cannot start with a closing delimiter
                        else -> PathSegment.Normal(char.toString()) // the single characters will be merged later
                    }
                }
                ParserState.INSIDE_CURLY_BRACKETS -> {
                    return when (char) {
                        '}' -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment.Parameter.SlashIgnoringParameter(name)
                        }
                        '{', '<', '>' -> throw MissingBracketsException(
                            segment,
                            rawPath
                        ) // cannot start another variable inside a variable
                        // wildcard is also okay inside a variable name
                        else -> {
                            pathNameAccumulator += char
                            null
                        }
                    }
                }
                ParserState.INSIDE_ANGLE_BRACKETS -> {
                    return when (char) {
                        '>' -> {
                            state = ParserState.NORMAL
                            val name = pathNameAccumulator.joinToString(separator = "")
                            pathNameAccumulator.clear()
                            PathSegment.Parameter.SlashAcceptingParameter(name)
                        }
                        '{', '}', '<' -> throw MissingBracketsException(
                            segment,
                            rawPath
                        ) // cannot start another variable inside a variable
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
                    lastAddition is PathSegment.Normal && pathSegment is PathSegment.Normal -> PathSegment.MultipleSegments(
                        acc.innerSegments.dropLast(1) + PathSegment.Normal(lastAddition.content + pathSegment.content)
                    )
                    else -> PathSegment.MultipleSegments(acc.innerSegments + pathSegment)
                }
            }
    }

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

    private val matchRegex = constructRegexList(matchEverySubPath, segments, regexSuffix) { it.asRegexString() }
    private val pathParamRegex =
        constructRegexList(matchEverySubPath, segments, regexSuffix) { it.asGroupedRegexString() }

    fun matches(url: String): Boolean = matchRegex.any { url matches it }

    fun extractPathParams(url: String): Map<String, String> {
        val index = matchRegex.indexOfFirst { url matches it }
        return pathParamNames.zip(values(pathParamRegex[index], url)) { name, value ->
            name to ContextUtil.urlDecode(value)
        }.toMap()
    }
}

fun createPathParser(path: String, ignoreTrailingSlashes: Boolean): PathParser = PathParser(path, ignoreTrailingSlashes)
