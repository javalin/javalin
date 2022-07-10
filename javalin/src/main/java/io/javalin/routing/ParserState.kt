package io.javalin.routing

private enum class ParserState {
    NORMAL,
    INSIDE_SLASH_IGNORING_BRACKETS,
    INSIDE_SLASH_ACCEPTING_BRACKETS
}

private val allDelimiters = setOf('{', '}', '<', '>')
private val adjacentViolations = listOf("*{", "*<", "}*", ">*")

internal fun convertSegment(segment: String, rawPath: String): PathSegment {
    val bracketsCount by lazy { segment.count { it in allDelimiters } }
    val wildcardCount by lazy { segment.count { it == '*' } }
    return when {
        bracketsCount % 2 != 0 -> throw MissingBracketsException(segment, rawPath)
        adjacentViolations.any { it in segment } -> throw WildcardBracketAdjacentException(segment, rawPath)
        segment == "*" -> PathSegment.Wildcard // a wildcard segment
        bracketsCount == 0 && wildcardCount == 0 -> createNormal(segment) // a normal segment, no params or wildcards
        bracketsCount == 2 && segment.isEnclosedBy('{', '}') -> createSlashIgnoringParam(segment.stripEnclosing('{', '}')) // simple path param (no slashes)
        bracketsCount == 2 && segment.isEnclosedBy('<', '>') -> createSlashAcceptingParam(segment.stripEnclosing('<', '>')) // simple path param (slashes)
        else -> parseAsPathSegment(segment, rawPath) // complicated path segment, need to parse
    }
}

private fun parseAsPathSegment(segment: String, rawPath: String): PathSegment {
    var state: ParserState = ParserState.NORMAL
    val pathNameAccumulator = mutableListOf<Char>()
    fun mapSingleChar(char: Char): PathSegment? = when (state) {
        ParserState.NORMAL -> when (char) {
            '*' -> PathSegment.Wildcard
            '{' -> {
                state = ParserState.INSIDE_SLASH_IGNORING_BRACKETS
                null
            }
            '<' -> {
                state = ParserState.INSIDE_SLASH_ACCEPTING_BRACKETS
                null
            }
            '}', '>' -> throw MissingBracketsException(
                segment,
                rawPath
            ) // cannot start with a closing delimiter
            else -> createNormal(char.toString()) // the single characters will be merged later
        }
        ParserState.INSIDE_SLASH_IGNORING_BRACKETS -> when (char) {
            '}' -> {
                state = ParserState.NORMAL
                val name = pathNameAccumulator.joinToString(separator = "")
                pathNameAccumulator.clear()
                createSlashIgnoringParam(name)
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
        ParserState.INSIDE_SLASH_ACCEPTING_BRACKETS -> when (char) {
            '>' -> {
                state = ParserState.NORMAL
                val name = pathNameAccumulator.joinToString(separator = "")
                pathNameAccumulator.clear()
                createSlashAcceptingParam(name)
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

    return segment.map(::mapSingleChar)
        .filterNotNull()
        .fold(PathSegment.MultipleSegments(emptyList())) { acc, pathSegment ->
            val lastAddition = acc.innerSegments.lastOrNull()
            when {
                lastAddition == null -> PathSegment.MultipleSegments(listOf(pathSegment))
                lastAddition is PathSegment.Wildcard && pathSegment is PathSegment.Wildcard -> acc
                lastAddition is PathSegment.Normal && pathSegment is PathSegment.Normal -> PathSegment.MultipleSegments(
                    acc.innerSegments.dropLast(1) + createNormal(lastAddition.content + pathSegment.content)
                )
                else -> PathSegment.MultipleSegments(acc.innerSegments + pathSegment)
            }
        }
}

private fun String.isEnclosedBy(prefix: Char, suffix: Char) = this.startsWith(prefix) && this.endsWith(suffix)
private fun String.stripEnclosing(prefix: Char, suffix: Char) = this.removePrefix(prefix.toString()).removeSuffix(suffix.toString())
