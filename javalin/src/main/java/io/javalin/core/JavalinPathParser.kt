package io.javalin.core

interface PathParserSpec {
    val segments: List<PathSegment>
    val pathParamNames: List<String>
    fun matches(url: String): Boolean
    fun extractPathParams(url: String): Map<String, String>
    fun extractSplats(url: String): List<String>
}

private class PathParserSpecImpl(path: String, ignoreTrailingSlashes: Boolean) : PathParserSpec {
    val impl = PathParser(path, ignoreTrailingSlashes)

    override val segments: List<PathSegment>
        get() = impl.segments

    override val pathParamNames: List<String>
        get() = impl.pathParamNames

    override fun matches(url: String): Boolean = impl.matches(url)

    override fun extractPathParams(url: String): Map<String, String> = impl.extractPathParams(url)
    override fun extractSplats(url: String): List<String> = impl.extractSplats(url)

}

fun createPathParser(path: String, ignoreTrailingSlashes: Boolean): PathParserSpec = PathParserSpecImpl(
    path,
    ignoreTrailingSlashes
)
