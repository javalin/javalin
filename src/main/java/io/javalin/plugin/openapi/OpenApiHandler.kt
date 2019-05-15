package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import io.swagger.v3.oas.models.OpenAPI

class OpenApiHandler(app: Javalin, val options: OpenApiOptions) : Handler {
    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()

    init {
        app.events { it.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) } }
    }

    fun createOpenAPISchema(): OpenAPI = JavalinOpenApi.createSchema(CreateSchemaOptions(
            handlerMetaInfoList = handlerMetaInfoList,
            createBaseConfiguration = options.createBaseConfiguration,
            defaultOperation = options.defaultOperation,
            modelConverterFactory = options.modelConverterFactory,
            packagePrefixesToScan = options.packagePrefixesToScan
    ))

    @OpenApi(
            responses = [
                OpenApiResponse("200", contentType = ContentType.JSON)
            ]
    )
    override fun handle(ctx: Context) {
        ctx.contentType(ContentType.JSON)
        ctx.result(options.toJsonMapper.map(createOpenAPISchema()))
    }
}
