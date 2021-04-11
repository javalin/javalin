package io.javalin.plugin.openapi

import io.javalin.http.Context
import io.swagger.v3.oas.models.OpenAPI


@FunctionalInterface
interface OpenApiModelModifier {
    fun apply(ctx : Context, model : OpenAPI) : OpenAPI
}

class NoOpOpenApiModelModifier : OpenApiModelModifier {
    override fun apply(ctx: Context, model: OpenAPI) = model
}

