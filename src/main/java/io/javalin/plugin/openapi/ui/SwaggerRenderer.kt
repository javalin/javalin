package io.javalin.plugin.openapi.ui

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.OpenApiOptions
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.intellij.lang.annotations.Language

class SwaggerOptions(path: String) : OpenApiUiOptions<SwaggerOptions>(path) {
    override val defaultTitle = "Swagger UI"
}

internal class SwaggerRenderer(private val openApiOptions: OpenApiOptions) : Handler {
    @OpenApi(
            responses = [
                OpenApiResponse("200", contentType = ContentType.HTML)
            ]
    )
    override fun handle(ctx: Context) {
        val swaggerUiOptions = openApiOptions.swagger!!
        val docsPath = openApiOptions.getFullDocumentationUrl(ctx)
        ctx.html(createSwaggerUiHtml(docsPath, swaggerUiOptions))
    }
}

private fun createSwaggerUiHtml(docsPath: String, options: SwaggerOptions): String {
    @Language("html")
    val html = """
<!-- HTML for static distribution bundle build -->
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>${options.createTitle()}</title>
  <link href="https://fonts.googleapis.com/css?family=Open+Sans:400,700|Source+Code+Pro:300,600|Titillium+Web:400,600,700" rel="stylesheet">
  <link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.22.1/swagger-ui.css" >
  <style>
    html
    {
      box-sizing: border-box;
      overflow: -moz-scrollbars-vertical;
      overflow-y: scroll;
    }
    *,
    *:before,
    *:after
    {
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

<script src="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.22.1/swagger-ui-bundle.js"> </script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/swagger-ui/3.22.1/swagger-ui-standalone-preset.js"> </script>
<script>
window.onload = function() {
  window.ui = SwaggerUIBundle({
        "dom_id": "#swagger-ui",
        deepLinking: true,
        presets: [
          SwaggerUIBundle.presets.apis,
          SwaggerUIStandalonePreset
        ],
        plugins: [
          SwaggerUIBundle.plugins.DownloadUrl
        ],
        layout: "StandaloneLayout",
        validatorUrl: "https://validator.swagger.io/validator",
        url: "$docsPath"
      })
}
</script>
</body>

</html>
    """.trimIndent()
    return html
}
