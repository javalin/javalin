package io.javalin.core

object JavalinPathParser {
    private val colonParserMode: () -> PathParserOptions = {
        PathParserOptions(
                openingDelimiter = ':',
                closingDelimiter = Char.MIN_VALUE,
                allowOptionalClosingDelimiter = true
        )
    }

    private val defaultParser = colonParserMode()
    private var parserImplementation = defaultParser
    val parserOptions: PathParserOptions
        get() = parserImplementation

    @JvmStatic
    fun useColonBasedParser() {
        parserImplementation = colonParserMode()
    }

    @JvmStatic
    fun useBracketsBasedParser() {
        parserImplementation = PathParserOptions()
    }

    @JvmStatic
    fun useDefaultParser() {
        parserImplementation = defaultParser
    }
}

interface PathParserSpec {
    val segments: List<PathSegment2>
    val pathParamNames: List<String>
    fun matches(url: String): Boolean
    fun extractPathParams(url: String): Map<String, String>
}

private class PathParserSpecImpl(path: String, options: PathParserOptions) : PathParserSpec {
    val impl = PathParser2(path, options)

    override val segments: List<PathSegment2>
        get() = impl.segments

    override val pathParamNames: List<String>
        get() = impl.pathParamNames

    override fun matches(url: String): Boolean = impl.matches(url)

    override fun extractPathParams(url: String): Map<String, String> = impl.extractPathParams(url)

}

fun createPathParser(path: String): PathParserSpec = PathParserSpecImpl(path, JavalinPathParser.parserOptions)
