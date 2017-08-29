/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.markdown

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

object JavalinCommonmarkPlugin {

    private var renderer: HtmlRenderer? = null
    private var parser: Parser? = null

    @JvmStatic
    fun configure(staticHtmlRenderer: HtmlRenderer, staticMarkdownParser: Parser) {
        renderer = staticHtmlRenderer
        parser = staticMarkdownParser
    }

    fun render(markdownFilePath: String): String {
        renderer = renderer ?: HtmlRenderer.builder().build()
        parser = parser ?: Parser.builder().build()
        val fileContent = JavalinCommonmarkPlugin::class.java.getResource(markdownFilePath).readText()
        return renderer!!.render(parser!!.parse(fileContent))
    }

}
