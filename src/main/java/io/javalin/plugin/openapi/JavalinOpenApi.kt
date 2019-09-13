package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.PathParser
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.plugin.PluginNotFoundException
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.dsl.applyMetaInfoList
import io.javalin.plugin.openapi.dsl.ensureDefaultResponse
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.dsl.updateComponents
import io.javalin.plugin.openapi.dsl.updatePaths
import io.javalin.plugin.openapi.jackson.JacksonModelConverterFactory
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI

class CreateSchemaOptions(
    val handlerMetaInfoList: List<HandlerMetaInfo>,

    /**
     * Create the base open api configuration.
     * This function will be called before the creation of every schema.
     */
    val initialConfigurationCreator: InitialConfigurationCreator,

    val default: DefaultDocumentation?,

    val modelConverterFactory: ModelConverterFactory = JacksonModelConverterFactory,

    val packagePrefixesToScan: Set<String>,

    val overridenPaths: Map<Pair<String, HttpMethod>, OpenApiDocumentation>? = emptyMap()
)

object JavalinOpenApi {
    @JvmStatic
    fun createSchema(javalin: Javalin): OpenAPI {
        val handler = try {
            javalin.config.getPlugin(OpenApiPlugin::class.java).openApiHandler
        } catch (e: PluginNotFoundException) {
            throw IllegalStateException("You need to register the \"OpenApiPlugin\" before you can create the OpenAPI schema")
        }
        return handler.createOpenAPISchema()
    }

    @JvmStatic
    fun createSchema(options: CreateSchemaOptions): OpenAPI {
        val baseConfiguration = options.initialConfigurationCreator.create()
        val modelConverter = options.modelConverterFactory.create()
        return runWithModelConverter(modelConverter) {
            baseConfiguration.apply {
                updateComponents {
                    applyMetaInfoList(options.handlerMetaInfoList, options)
                }

                updatePaths {
                    applyMetaInfoList(options.handlerMetaInfoList, options)
                    options.overridenPaths?.forEach { (path, documentation) ->
                        applyMetaInfoList(options.handlerMetaInfoList.filter {
                            PathParser(path.first).matches(it.path) && it.httpMethod.name == path.second.name
                        }.ifEmpty {
                            listOf(HandlerMetaInfo(HandlerType.valueOf(path.second.name), path.first, Handler { }, emptySet()))
                        }.map { it.copy(handler = documented(documentation, Handler { })) }, options)
                    }
                    ensureDefaultResponse()
                }
            }
        }
    }
}

/**
 * This function temporary sets the ModelConverter.
 * This influences how the classes are converted to the open api schema.
 */
private fun <T> runWithModelConverter(modelConverter: ModelConverter, run: () -> T): T {
    ModelConverters.getInstance().addConverter(modelConverter)
    val result = run()
    ModelConverters.getInstance().removeConverter(modelConverter)
    return result
}
