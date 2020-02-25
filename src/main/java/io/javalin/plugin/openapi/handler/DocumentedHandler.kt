package io.javalin.plugin.openapi.handler

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation

class DocumentedHandler(
        val documentation: OpenApiDocumentation,
        private val handler: Handler
) : Handler {
    override fun handle(ctx: Context) = handler.handle(ctx)
}
