package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.json.JavalinJackson
import io.swagger.v3.oas.models.OpenAPI

class OpenApiHandler(app: Javalin, val options: OpenApiOptions) : Handler {
    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()

    init {
        app.events { it.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) } }
    }

    fun createOpenAPISchema(): OpenAPI = JavalinOpenApi.createSchema(CreateSchemaOptions(
            handlerMetaInfoList = handlerMetaInfoList,
            objectMapper = JavalinJackson.getObjectMapper(),
            createBaseConfiguration = options.createBaseConfiguration
    ))

    override fun handle(ctx: Context) {
        ctx.json(createOpenAPISchema())
    }
}
