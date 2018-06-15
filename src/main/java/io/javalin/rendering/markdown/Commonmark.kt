/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.markdown

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.rendering.FileRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object JavalinCommonmark : FileRenderer {

    private var renderer: HtmlRenderer? = null
    private var parser: Parser? = null

    @JvmStatic
    fun configure(staticHtmlRenderer: HtmlRenderer, staticMarkdownParser: Parser) {
        renderer = staticHtmlRenderer
        parser = staticMarkdownParser
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent(OptionalDependency.COMMONMARK)
        renderer = renderer ?: HtmlRenderer.builder().build()
        parser = parser ?: Parser.builder().build()
        val fileContent = JavalinCommonmark::class.java.getResource(filePath).readText()
        return renderer!!.render(parser!!.parse(fileContent))
    }

}
