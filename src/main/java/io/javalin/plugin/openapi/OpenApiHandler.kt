package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.PathParser
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApi
import io.swagger.util.Yaml
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(OpenApiHandler::class.java)

class OpenApiHandler(app: Javalin, val options: OpenApiOptions) : Handler {
    private val handlerMetaInfoList = mutableListOf<HandlerMetaInfo>()
    private var schema: OpenAPI? = null

    init {
        app.events {
            it.handlerAdded { handlerInfo ->
                if (handlerInfo.httpMethod.isHttpMethod()) {
                    handlerMetaInfoList.add(handlerInfo)
                    schema = null
                }
            }
        }
    }

    fun createOpenAPISchema(): OpenAPI {
        val schema = JavalinOpenApi.createSchema(
                CreateSchemaOptions(
                        handlerMetaInfoList = handlerMetaInfoList.filter { handler ->
                            options.ignoredPaths.none { (path, methods) ->
                                PathParser(path).matches(handler.path) && methods.any { method ->
                                    // HttpMethod is implemented two times :(
                                    method.name == handler.httpMethod.name
                                }
                            }
                        },
                        initialConfigurationCreator = options.initialConfigurationCreator,
                        default = options.default,
                        modelConverterFactory = options.modelConverterFactory,
                        packagePrefixesToScan = options.packagePrefixesToScan,
                        overridenPaths = options.overriddenDocumentation
                )
        )

        if (options.validateSchema) {
            Util.ensureDependencyPresent(OptionalDependency.SWAGGERPARSER)
            val parsedSchema = OpenAPIV3Parser().readContents(Yaml.mapper().writeValueAsString(schema))

            if (parsedSchema.messages.isNotEmpty()) {
                logger.warn("The generated OpenApi specification is not valid")

                parsedSchema.messages.forEach {
                    logger.warn(it)
                }
            }
        }

        return schema
    }

    // This function is synchronized because an attacker can trigger the openapi schema generation very often
    // It is ensured, that the schema is generated only maximal once after adding handlers
    // See https://github.com/tipsy/javalin/pull/736#discussion_r322016515
    @Synchronized
    private fun initializeSchemaSynchronized(): OpenAPI {
        return (schema ?: createOpenAPISchema()).apply { schema = this }
    }

    @OpenApi(ignore = true)
    override fun handle(ctx: Context) {
        ctx.contentType(ContentType.JSON)
        ctx.header(Header.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, "GET")
        ctx.result(options.toJsonMapper.map(initializeSchemaSynchronized()))
    }
}
