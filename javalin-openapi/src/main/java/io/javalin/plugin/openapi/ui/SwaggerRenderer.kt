package io.javalin.plugin.openapi.ui

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.annotations.OpenApi
import org.intellij.lang.annotations.Language

class SwaggerOptions(path: String) : OpenApiUiOptions<SwaggerOptions>(path) {
    override val defaultTitle = "Swagger UI"
}

internal class SwaggerRenderer(private val openApiOptions: OpenApiOptions) : Handler {
    @OpenApi(ignore = true)
    override fun handle(ctx: Context) {
        val swaggerUiOptions = openApiOptions.swagger!!
        val docsPath = openApiOptions.getFullDocumentationUrl(ctx)
        ctx.html(createSwaggerUiHtml(ctx, docsPath, swaggerUiOptions))
    }
}

private fun createSwaggerUiHtml(ctx: Context, docsPath: String, options: SwaggerOptions): String {
    val publicBasePath = Util.getWebjarPublicPath(ctx, OptionalDependency.SWAGGERUI)

    @Language("html")
    val html = """
        <!-- HTML for static distribution bundle build -->
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>${options.createTitle()}</title>
                <link rel="stylesheet" type="text/css" href="$publicBasePath/swagger-ui.css" >
                <link rel="icon" type="image/png" href="$publicBasePath/favicon-32x32.png" sizes="32x32" />
                <link rel="icon" type="image/png" href="$publicBasePath/favicon-16x16.png" sizes="16x16" />
                <style>
                    html {
                        box-sizing: border-box;
                        overflow: -moz-scrollbars-vertical;
                        overflow-y: scroll;
                    }
                    *, *:before, *:after {
                        box-sizing: inherit;
                    }
                    body {
                        margin:0;
                        background: #fafafa;
                    }
                </style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="$publicBasePath/swagger-ui-bundle.js"> </script>
                <script src="$publicBasePath/swagger-ui-standalone-preset.js"> </script>
                <script>
                window.onload = function() {
                    window.ui = SwaggerUIBundle({
                        url: "$docsPath",
                        dom_id: "#swagger-ui",
                        deepLinking: true,
                        presets: [
                          SwaggerUIBundle.presets.apis,
                          SwaggerUIStandalonePreset
                        ],
                        plugins: [
                          SwaggerUIBundle.plugins.DownloadUrl
                        ],
                        layout: "StandaloneLayout"
                      })
                }
                </script>
            </body>
        </html>
    """.trimIndent()
    return html
}
