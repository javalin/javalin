package io.javalin.router.matcher

import io.javalin.router.matcher.PathSegment.MultipleSegments
import io.javalin.router.matcher.PathSegment.Normal
import io.javalin.router.matcher.PathSegment.Normal.RegexAllowed
import io.javalin.router.matcher.PathSegment.Normal.RegexEscaped
import io.javalin.router.matcher.PathSegment.Parameter
import io.javalin.router.matcher.PathSegment.Parameter.SlashAcceptingParameter
import io.javalin.router.matcher.PathSegment.Parameter.SlashIgnoringParameter
import io.javalin.router.matcher.PathSegment.Wildcard

private fun String.grouped() = "($this)"

sealed class PathSegment {

    internal abstract fun asRegexString(): String

    internal abstract fun asGroupedRegexString(): String

    sealed class Normal(val content: String) : PathSegment() {
        // do not group static content
        class RegexEscaped(content: String) : Normal(content) {
            override fun asRegexString(): String = Regex.escape(content)
            override fun asGroupedRegexString(): String = Regex.escape(content)
        }

        class RegexAllowed(content: String) : Normal(content) {
            override fun asRegexString(): String = content
            override fun asGroupedRegexString(): String = content
        }
    }

    sealed class Parameter(val name: String) : PathSegment() {
        class SlashIgnoringParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = "[^/]+?" // Accept everything except slash
            override fun asGroupedRegexString(): String = asRegexString().grouped()
        }

        class SlashAcceptingParameter(name: String) : Parameter(name) {
            override fun asRegexString(): String = ".+?" // Accept everything
            override fun asGroupedRegexString(): String = asRegexString().grouped()
        }
    }

    object Wildcard : PathSegment() {
        override fun asRegexString(): String = ".*?" // Accept everything
        override fun asGroupedRegexString(): String = asRegexString()
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
        override fun asRegexString(): String = regex
        override fun asGroupedRegexString(): String = groupedRegex
    }

}

internal fun createNormal(string: String, enableRegex: Boolean = false) = if (enableRegex) {
    RegexAllowed(string)
} else {
    RegexEscaped(string)
}

internal fun createSlashIgnoringParam(string: String) = SlashIgnoringParameter(string)
internal fun createSlashAcceptingParam(string: String) = SlashAcceptingParameter(string)

internal fun PathSegment.pathParamNames(): List<String> {
    return when (this) {
        is Normal, is Wildcard -> emptyList()
        is Parameter -> listOf(this.name)
        is MultipleSegments -> this.innerSegments.filterIsInstance<Parameter>().map { it.name }
    }
}
