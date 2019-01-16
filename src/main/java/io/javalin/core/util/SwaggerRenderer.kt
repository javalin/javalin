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
    private val swaggerVersion = OptionalDependency.SWAGGERUI.version

    override fun handle(ctx: Context) {
        if (Util.getResourceUrl("META-INF/resources/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css") == null) {
            log.warn(Util.missingDependencyMessage(OptionalDependency.SWAGGERUI))
            throw InternalServerErrorResponse(Util.missingDependencyMessage(OptionalDependency.SWAGGERUI))
        }
        if (ctx.queryParam("spec") != null)
            ctx.result(Util.getResourceUrl(ctx.queryParam("spec")!!)!!.readText())
        else ctx.html("""
            <head>
                <meta charset="UTF-8">
                <title>Swagger UI</title>
                <link rel="icon" type="image/png" href="${ctx.contextPath()}/webjars/swagger-ui/$swaggerVersion/favicon-16x16.png" sizes="16x16" />
                <link rel="stylesheet" href="${ctx.contextPath()}/webjars/swagger-ui/$swaggerVersion/swagger-ui.css" >
                <script src="${ctx.contextPath()}/webjars/swagger-ui/$swaggerVersion/swagger-ui-bundle.js"></script>
                <style>body{background:#fafafa;}</style>
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script>
                    window.ui = SwaggerUIBundle({
                        url: "${ctx.matchedPath}?spec=$filePath",
                        dom_id: "#swagger-ui",
                        deepLinking: true,
                        presets: [SwaggerUIBundle.presets.apis],
                    });
                </script>
            </body>""".trimIndent()
        )
    }

}
