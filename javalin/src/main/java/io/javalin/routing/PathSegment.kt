package io.javalin.routing

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
    PathSegment.Normal.RegexAllowed(string)
} else {
    PathSegment.Normal.RegexEscaped(string)
}

internal fun createSlashIgnoringParam(string: String) = PathSegment.Parameter.SlashIgnoringParameter(string)
internal fun createSlashAcceptingParam(string: String) = PathSegment.Parameter.SlashAcceptingParameter(string)

internal fun PathSegment.pathParamNames(): List<String> {
    return when (this) {
        is PathSegment.Normal, is PathSegment.Wildcard -> emptyList()
        is PathSegment.Parameter -> listOf(this.name)
        is PathSegment.MultipleSegments -> this.innerSegments.filterIsInstance<PathSegment.Parameter>().map { it.name }
    }
}
