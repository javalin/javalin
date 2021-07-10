package io.javalin.jte.precompiled.kte

class JtemultipleparamsGenerated {
    companion object {
        @JvmField
        val JTE_NAME = "kte/multiple-params.kte"
        @JvmField
        val JTE_LINE_INFO = intArrayOf(0, 0, 0, 0, 0, 2, 2, 2, 2, 2, 2, 2, 3)
        @JvmStatic
        fun render(jteOutput: gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor: gg.jte.html.HtmlInterceptor?, one: String, two: String) {
            jteOutput.writeContent("<h1>")
            jteOutput.setContext("h1", null)
            jteOutput.writeUserContent(one)
            jteOutput.writeContent(" ")
            jteOutput.setContext("h1", null)
            jteOutput.writeUserContent(two)
            jteOutput.writeContent("!</h1>\n")
        }

        @JvmStatic
        fun renderMap(jteOutput: gg.jte.html.HtmlTemplateOutput, jteHtmlInterceptor: gg.jte.html.HtmlInterceptor?, params: Map<String, Any>) {
            val one = params.get("one") as String
            val two = params.get("two") as String
            render(jteOutput, jteHtmlInterceptor, one, two);
        }
    }
}
