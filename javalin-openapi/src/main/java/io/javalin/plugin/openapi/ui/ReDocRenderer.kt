package io.javalin.plugin.openapi.ui

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.annotations.OpenApi

class ReDocOptions @JvmOverloads constructor(
    path: String,
    internal val optionsObject: RedocOptionsObject = RedocOptionsObject()
) : OpenApiUiOptions<ReDocOptions>(path) {
    
    override val defaultTitle = "ReDoc"
}


internal class ReDocRenderer(private val openApiOptions: OpenApiOptions) : Handler {
    @OpenApi(ignore = true)
    override fun handle(ctx: Context) {
        val reDocOptions = openApiOptions.reDoc!!
        val docsPath = openApiOptions.getFullDocumentationUrl(ctx)
        ctx.html(createReDocHtml(ctx, docsPath, reDocOptions))
    }
}

private fun createReDocHtml(ctx: Context, docsPath: String, redocOptions: ReDocOptions): String {
    val publicBasePath = Util.getWebjarPublicPath(ctx, OptionalDependency.REDOC)
    val options = redocOptions.optionsObject

    return """
    |<!DOCTYPE html>
    |<html>
    |  <head>
    |    <title>${redocOptions.createTitle()}</title>
    |    <!-- Needed for adaptive design -->
    |    <meta charset="utf-8"/>
    |    <meta name="viewport" content="width=device-width, initial-scale=1">
    |    <link href="https://fonts.googleapis.com/css?family=Montserrat:300,400,700|Roboto:300,400,700" rel="stylesheet">
    |    <!-- ReDoc doesn't change outer page styles -->
    |    <style>body{margin:0;padding:0;}</style>
    |  </head>
    |  <body>
    |  <redoc id='redoc'></redoc>
    |  <script src="$publicBasePath/bundles/redoc.standalone.js"></script>
    |  <script>
    |   window.onload = () => {
    |     Redoc.init('$docsPath', ${options.json()}, document.getElementById('redoc'))
    |   }
    | </script>
    |  </body>
    |</html>
    |""".trimMargin()
}
