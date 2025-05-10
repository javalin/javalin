package io.javalin.router.matcher

import io.javalin.router.matcher.ParserState.INSIDE_SLASH_ACCEPTING_BRACKETS
import io.javalin.router.matcher.ParserState.INSIDE_SLASH_IGNORING_BRACKETS
import io.javalin.router.matcher.ParserState.NORMAL
import io.javalin.router.matcher.PathSegment.MultipleSegments
import io.javalin.router.matcher.PathSegment.Normal
import io.javalin.router.matcher.PathSegment.Wildcard

import io.javalin.util.javalinLazy
import kotlin.LazyThreadSafetyMode.NONE

import io.javalin.util.JavalinLogger

private enum class ParserState {
    NORMAL,
    INSIDE_SLASH_IGNORING_BRACKETS,
    INSIDE_SLASH_ACCEPTING_BRACKETS
}

private val allDelimiters = setOf('{', '}', '<', '>')
private val adjacentViolations = listOf("*{", "*<", "}*", ">*")

internal fun convertSegment(segment: String, rawPath: String): PathSegment {
    val bracketsCount by javalinLazy { segment.count { it in allDelimiters } }
    val wildcardCount by javalinLazy { segment.count { it == '*' } }
    return when {
        bracketsCount % 2 != 0 -> throw MissingBracketsException(segment, rawPath)
        adjacentViolations.any { it in segment } -> throw WildcardBracketAdjacentException(segment, rawPath)
        segment == "*" -> Wildcard // a wildcard segment
        bracketsCount == 0 && wildcardCount == 0 -> createNormal(segment) // a normal segment, no params or wildcards
        bracketsCount == 2 && segment.isEnclosedBy('{', '}') -> createSlashIgnoringParam(segment.stripEnclosing('{', '}')) // simple path param (no slashes)
        bracketsCount == 2 && segment.isEnclosedBy('<', '>') -> createSlashAcceptingParam(segment.stripEnclosing('<', '>')) // simple path param (slashes)
        else -> parseAsPathSegment(segment, rawPath) // complicated path segment, need to parse
    }
}

private fun parseAsPathSegment(segment: String, rawPath: String): PathSegment {
    JavalinLogger.info("parseAsPathSegment, segment : " + segment)
    var state: ParserState = NORMAL
    val pathNameAccumulator = mutableListOf<Char>()
    fun mapSingleChar(char: Char): PathSegment? = when (state) {
        NORMAL -> when (char) {
            '*' -> Wildcard
            '{' -> {
                state = INSIDE_SLASH_IGNORING_BRACKETS
                null
            }

            '<' -> {
                state = INSIDE_SLASH_ACCEPTING_BRACKETS
                null
            }

            '}', '>' -> throw MissingBracketsException(
                segment,
                rawPath
            ) // cannot start with a closing delimiter
            else -> createNormal(char.toString()) // the single characters will be merged later
        }

        INSIDE_SLASH_IGNORING_BRACKETS -> when (char) {
            '}' -> {
                state = NORMAL
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

        INSIDE_SLASH_ACCEPTING_BRACKETS -> when (char) {
            '>' -> {
                state = NORMAL
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
        .fold(MultipleSegments(emptyList())) { acc, pathSegment ->
            val lastAddition = acc.innerSegments.lastOrNull()
            when {
                lastAddition == null -> MultipleSegments(listOf(pathSegment))
                lastAddition is Wildcard && pathSegment is Wildcard -> acc
                lastAddition is Normal && pathSegment is Normal -> MultipleSegments(
                    acc.innerSegments.dropLast(1) + createNormal(lastAddition.content + pathSegment.content)
                )

                else -> MultipleSegments(acc.innerSegments + pathSegment)
            }
        }
}

private fun String.isEnclosedBy(prefix: Char, suffix: Char) = this.startsWith(prefix) && this.endsWith(suffix)
private fun String.stripEnclosing(prefix: Char, suffix: Char) = this.removePrefix(prefix.toString()).removeSuffix(suffix.toString())
