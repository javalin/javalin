package io.javalin.core

import io.javalin.http.util.ContextUtil

class MissingBracketsException(val segment: String, val path: String) : RuntimeException(
        "This segment '$segment' is missing some brackets! Found in path '$path'"
)

class WildcardBracketAdjacentException(val segment: String, val path: String) : RuntimeException(
        "Wildcard and a path parameter bracket are adjacent in segment '$segment' of path '$path'. This is forbidden"
)

class PathParser2(private val path: String) {

    internal val segments: List<PathSegment2> = path.split("/")
            .filter { it.isNotEmpty() }
            .map(::convertSegment)

    private fun convertSegment(segment: String): PathSegment2 {
        val brackets = segment.count { it == '{' || it == '}' }
        val wildcards = segment.count { it == '*' }
        // avoid parsing malformed paths
        if (brackets % 2 != 0) {
            throw MissingBracketsException(segment, path)
        }
        if ("*{" in segment || "}*" in segment) {
            throw WildcardBracketAdjacentException(segment, path)
        }

        // early return for the most common scenarios
        when {
            // just a Parameter in this segment
            // checking the number of brackets enforces a path parameter name without those brackets
            brackets == 2 && segment.startsWith("{") && segment.endsWith("}") -> return PathSegment2.Parameter(segment.removePrefix("{").removeSuffix("}"))
            segment == "*" -> return PathSegment2.Wildcard
            // no special characters
            brackets == 0 && wildcards == 0 -> return PathSegment2.Normal(segment)
        }

        return parseAsPathSegment(segment)
    }

    private fun parseAsPathSegment(segment: String): PathSegment2 {
        var insideBrackets = false
        val pathNameAccumulator = mutableListOf<Char>()
        fun mapSingleChar(char: Char): PathSegment2? {
            return if (!insideBrackets) {
                when (char) {
                    '*' -> PathSegment2.Wildcard
                    '{' -> {
                        insideBrackets = true
                        null
                    }
                    '}' -> throw MissingBracketsException(segment, path) // cannot start with a closing bracket
                    else -> PathSegment2.Normal(char.toString()) // the single characters will be merged later
                }
            } else {
                when (char) {
                    '{' -> throw MissingBracketsException(segment, path) // cannot start another variable inside a variable
                    '}' -> {
                        insideBrackets = false
                        val name = pathNameAccumulator.joinToString(separator = "")
                        pathNameAccumulator.clear()
                        PathSegment2.Parameter(name)
                    }
                    // wildcard is also okay inside a variable name
                    else -> {
                        pathNameAccumulator += char
                        null
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
    // TODO: replace this property by making Parameter a sealed class itself
    internal open val isParameter: Boolean = false

    internal fun asGroupedRegexString(): String {
        return if (isParameter) {
            "(${this.asRegexString()})"
        } else {
            this.asRegexString()
        }
    }

    class Normal(val content: String) : PathSegment2() {
        override fun asRegexString(): String = content
    }

    class Parameter(val name: String) : PathSegment2() {
        override fun asRegexString(): String = "[^/]+?" // Accept everything except slash
        override val isParameter: Boolean = true
    }

    object Wildcard : PathSegment2() {
        override fun asRegexString(): String = ".*?" // Accept everything
    }

    class MultipleSegments(segments: List<PathSegment2>) : PathSegment2() {
        // TODO: maybe throw an exception instead of silently ignoring MultipleSegments inside MultipleSegments
        val innerSegments = segments.filterNot { it is MultipleSegments }

        private val regex: String = innerSegments.joinToString(separator = "") { it.asRegexString() }
        override fun asRegexString(): String = regex
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
