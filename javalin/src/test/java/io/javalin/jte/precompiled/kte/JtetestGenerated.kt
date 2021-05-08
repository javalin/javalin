package io.javalin.jte.precompiled.kte

import io.javalin.TestTemplates.JteTestPage

class JtetestGenerated {
    companion object {
        @JvmField
        val JTE_NAME = "kte/test.kte"
        @JvmField
        val JTE_LINE_INFO = intArrayOf(0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3)
        @JvmStatic
        fun render(jteOutput: gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor: gg.jte.html.HtmlInterceptor?, page: JteTestPage) {
            jteOutput.writeContent("<h1>")
            jteOutput.setContext("h1", null)
            jteOutput.writeUserContent(page.hello)
            jteOutput.writeContent(" ")
            jteOutput.setContext("h1", null)
            jteOutput.writeUserContent(page.world)
            jteOutput.writeContent("!</h1>\n")
        }

        @JvmStatic
        fun renderMap(jteOutput: gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor: gg.jte.html.HtmlInterceptor?, params: Map<String, Any>) {
            val page = params.get("page") as JteTestPage
            render(jteOutput, jteHtmlInterceptor, page);
        }
    }
}
