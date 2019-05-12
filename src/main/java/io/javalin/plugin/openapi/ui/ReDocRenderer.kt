package io.javalin.plugin.openapi.ui

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.intellij.lang.annotations.Language

class ReDocOptions(path: String) : OpenApiUiOptions<ReDocOptions>(path) {
    override val defaultTitle = "ReDoc"
}

internal class ReDocRenderer(private val openApiOptions: OpenApiOptions) : Handler {
    @OpenApi(
            responses = [
                OpenApiResponse("200", contentType = ContentType.HTML)
            ]
    )
    override fun handle(ctx: Context) {
        val reDocOptions = openApiOptions.reDoc!!
        val docsPath = openApiOptions.getFullDocumentationUrl(ctx)
        ctx.html(createReDocHtml(docsPath, reDocOptions))
    }
}

private fun createReDocHtml(docsPath: String, options: ReDocOptions): String {
    @Language("html")
    val html = """
<!DOCTYPE html>
<html>
  <head>
    <title>${options.createTitle()}</title>
    <!-- needed for adaptive design -->
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css?family=Montserrat:300,400,700|Roboto:300,400,700" rel="stylesheet">

    <!--
    ReDoc doesn't change outer page styles
    -->
    <style>
      body {
        margin: 0;
        padding: 0;
      }
    </style>
  </head>
  <body>
    <redoc spec-url="$docsPath"></redoc>
    <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"> </script>
  </body>
</html>
    """.trimIndent()
    return html
}
