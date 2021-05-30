package io.javalin.core

object JavalinPathParser {
    private val colonParserMode: () -> PathParserOptions = {
        PathParserOptions(
                openingDelimiterType1 = ':',
                closingDelimiterType1 = Char.MIN_VALUE,
                allowOptionalClosingDelimiter = true,
                // Slash accepting parameters are not supported for colon parser mode
                openingDelimiterType2 = Char.MIN_VALUE,
                closingDelimiterType2 = Char.MIN_VALUE
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

    @JvmStatic
    fun custom(options: PathParserOptions) {
        parserImplementation = options
    }
}

interface PathParserSpec {
    val segments: List<PathSegment>
    val pathParamNames: List<String>
    fun matches(url: String): Boolean
    fun extractPathParams(url: String): Map<String, String>
    fun extractSplats(url: String): List<String>
}

private class PathParserSpecImpl(path: String, options: PathParserOptions, ignoreTrailingSlashes: Boolean) : PathParserSpec {
    val impl = PathParser(path, options, ignoreTrailingSlashes)

    override val segments: List<PathSegment>
        get() = impl.segments

    override val pathParamNames: List<String>
        get() = impl.pathParamNames

    override fun matches(url: String): Boolean = impl.matches(url)

    override fun extractPathParams(url: String): Map<String, String> = impl.extractPathParams(url)
    override fun extractSplats(url: String): List<String> = impl.extractSplats(url)

}

fun createPathParser(path: String, ignoreTrailingSlashes: Boolean): PathParserSpec = PathParserSpecImpl(path, JavalinPathParser.parserOptions, ignoreTrailingSlashes)
