package io.javalin.plugin.openapi.dsl

import io.javalin.http.Context
import io.javalin.http.Handler

class DocumentedHandler(
        val documentation: OpenApiDocumentation,
        private val handler: Handler
) : Handler {
    override fun handle(ctx: Context) = handler.handle(ctx)
}
