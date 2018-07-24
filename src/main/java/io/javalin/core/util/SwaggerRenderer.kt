/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Context
import io.javalin.Handler
import io.javalin.InternalServerErrorResponse
import org.slf4j.LoggerFactory

class SwaggerRenderer(val filePath: String) : Handler {

    private val log = LoggerFactory.getLogger(SwaggerRenderer::class.java)
    private val classLoader = this.javaClass.classLoader
    private val swaggerVersion = OptionalDependency.SWAGGERUI.version

    override fun handle(ctx: Context) {
        if (classLoader.getResource("META-INF/resources/webjars/swagger-ui/3.17.1/swagger-ui.css") == null) {
            log.warn(Util.missingDependencyMessage(OptionalDependency.SWAGGERUI))
            throw InternalServerErrorResponse(Util.missingDependencyMessage(OptionalDependency.SWAGGERUI))
        }
        if (ctx.queryParam("spec") != null)
            ctx.result(classLoader.getResource(ctx.queryParam("spec")).readText())
        else ctx.html("""
            <head>
                <meta charset="UTF-8">
                <title>Swagger UI</title>
                <link rel="stylesheet" href="/webjars/swagger-ui/$swaggerVersion/swagger-ui.css" >
                <style>
                  html {
                    box-sizing: border-box;
                    overflow-y: scroll;
                  }
                  *, *:before, *:after {
                    box-sizing: inherit;
                  }
                  body {
                    margin: 0;
                    background: #fafafa;
                  }
                </style>
                </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="/webjars/swagger-ui/$swaggerVersion/swagger-ui-bundle.js"> </script>
                <script src="/webjars/swagger-ui/$swaggerVersion/swagger-ui-standalone-preset.js"></script>
                <script>
                window.onload = function() {
                  const ui = SwaggerUIBundle({
                    url: "${ctx.matchedPath}?spec=${filePath}",
                    dom_id: '#swagger-ui',
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
                  window.ui = ui
                }
                </script>
            </body>""".trimIndent()
        )
    }

}
