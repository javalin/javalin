package io.javalin.core

enum class PathParserImplementation {
    COLON_BASED_PARSER,
    BRACKETS_BASED_PARSER
}

object JavalinPathParser {
    private val defaultParser = PathParserImplementation.COLON_BASED_PARSER
    private var parserImplementation = defaultParser

    @JvmStatic
    fun useColonBasedParser() {
        parserImplementation = PathParserImplementation.COLON_BASED_PARSER
    }

    @JvmStatic
    fun useBracketsBasedParser() {
        parserImplementation = PathParserImplementation.BRACKETS_BASED_PARSER
    }

    @JvmStatic
    fun useDefaultParser() {
        parserImplementation = defaultParser
    }

    internal val isColonBasedParser: Boolean
        get() = this.parserImplementation == PathParserImplementation.COLON_BASED_PARSER
}

internal interface PathParserSpec {
    fun matches(url: String): Boolean
    fun extractPathParams(url: String): Map<String, String>
}

private class ColonBasedParser(path: String) : PathParserSpec {
    val impl = PathParser(path)

    override fun matches(url: String): Boolean = impl.matches(url)

    override fun extractPathParams(url: String): Map<String, String> = impl.extractPathParams(url)
}

private class BracketsBasedParser(path: String) : PathParserSpec {
    val impl = PathParser2(path)

    override fun matches(url: String): Boolean = impl.matches(url)

    override fun extractPathParams(url: String): Map<String, String> = impl.extractPathParams(url)

}

internal fun createPathParser(path: String): PathParserSpec = if (JavalinPathParser.isColonBasedParser) {
    ColonBasedParser(path)
} else {
    BracketsBasedParser(path)
}
