package io.javalin.plugin.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.plugin.openapi.dsl.applyMetaInfoList
import io.javalin.plugin.openapi.dsl.updateComponents
import io.javalin.plugin.openapi.dsl.updatePaths
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.OpenAPI

class CreateSchemaOptions(
        val objectMapper: ObjectMapper,
        val handlerMetaInfoList: List<HandlerMetaInfo>,

        /**
         * Create the base open api configuration.
         * This function will be called before the creation of every schema.
         */
        val createBaseConfiguration: CreateBaseConfiguration
)

object JavalinOpenApi {
    fun createSchema(javalin: Javalin): OpenAPI {
        val handler = javalin.config.inner.openApiHandler
        return handler?.createOpenAPISchema()
                ?: throw IllegalStateException("You need to activate the \"enableOpenApi\" option before you can create the OpenAPI schema");
    }

    fun createSchema(options: CreateSchemaOptions): OpenAPI {
        val baseConfiguration = options.createBaseConfiguration.create()
        return runWithObjectMapper(options.objectMapper) {
            baseConfiguration.apply {
                updateComponents {
                    applyMetaInfoList(options.handlerMetaInfoList)
                }
                updatePaths {
                    applyMetaInfoList(options.handlerMetaInfoList)
                }
            }
        }
    }
}

/**
 * This function temporary sets the ModelResolve to the given object mapper.
 * This is required, so the classes are correctly parse to OpenAPI schemas
 */
private fun <T> runWithObjectMapper(objectMapper: ObjectMapper, run: () -> T): T {
    val modelResolver = ModelResolver(objectMapper)
    ModelConverters.getInstance().addConverter(modelResolver)
    val result = run()
    ModelConverters.getInstance().removeConverter(modelResolver)
    return result
}
