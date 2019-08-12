package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.swagger.v3.oas.models.OpenAPI

class OpenApiHandler(app: Javalin, val options: OpenApiOptions) : Handler {
    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()

    init {
        app.events { it.handlerAdded { handlerInfo -> handlerMetaInfoList.add(handlerInfo) } }
    }

    fun createOpenAPISchema(): OpenAPI = JavalinOpenApi.createSchema(CreateSchemaOptions(
            handlerMetaInfoList = handlerMetaInfoList,
            initialConfigurationCreator = options.initialConfigurationCreator,
            default = options.default,
            modelConverterFactory = options.modelConverterFactory,
            packagePrefixesToScan = options.packagePrefixesToScan
    ))

    @OpenApi(ignore = true)
    override fun handle(ctx: Context) {
        ctx.contentType(ContentType.JSON)
        ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, "GET")
        ctx.result(options.toJsonMapper.map(createOpenAPISchema()))
    }
}
