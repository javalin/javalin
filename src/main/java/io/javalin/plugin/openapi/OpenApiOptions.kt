package io.javalin.plugin.openapi

import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.plugin.json.ToJsonMapper
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.jackson.JacksonModelConverterFactory
import io.javalin.plugin.openapi.jackson.JacksonToJsonMapper
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.javalin.plugin.openapi.utils.LazyDefaultValue
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info

class OpenApiOptions constructor(val initialConfigurationCreator: InitialConfigurationCreator) {
    /** If not null, creates a GET route to get the schema as a json */
    var path: String? = null
    var roles: Set<Role> = setOf()
    /**
     * If not null, creates a GET route to the swagger ui
     * @see <a href="https://swagger.io/tools/swagger-ui/">https://swagger.io/tools/swagger-ui/</a>
     */
    var swagger: SwaggerOptions? = null
    /**
     * If not null, creates a GET route to the reDoc ui
     * @see <a href="https://github.com/Rebilly/ReDoc">https://github.com/Rebilly/ReDoc</a>
     */
    var reDoc: ReDocOptions? = null
    /**
     * Function that is applied to every new operation.
     * You can use this to set defaults (like a 500 response).
     */
    var default: DefaultDocumentation? = null
    /**
     * Creates a model converter, which converts a class to an open api schema.
     * Defaults to the jackson converter.
     */
    var modelConverterFactory: ModelConverterFactory by LazyDefaultValue { JacksonModelConverterFactory }
    /**
     * The json mapper for creating the object api schema json. This is separated from
     * the default JavalinJson mappers.
     */
    var toJsonMapper: ToJsonMapper by LazyDefaultValue { JacksonToJsonMapper }
    /**
     * A list of package prefixes to scan for annotations.
     */
    var packagePrefixesToScan = mutableSetOf<String>()

    constructor(info: Info) : this(InitialConfigurationCreator { OpenAPI().info(info) })

    fun path(value: String) = apply { path = value }

    fun swagger(value: SwaggerOptions) = apply { swagger = value }

    fun reDoc(value: ReDocOptions) = apply { reDoc = value }

    fun roles(value: Set<Role>) = apply { roles = value }

    fun defaultDocumentation(value: DefaultDocumentation) = apply { default = value }
    fun defaultDocumentation(apply: (documentation: OpenApiDocumentation) -> Unit) = apply {
        default = object : DefaultDocumentation {
            override fun apply(documentation: OpenApiDocumentation) = apply(documentation)
        }
    }

    /**
     * Activate annotation scanning for specific package prefixes.
     * This will search for `OpenApi` annotations which define the `path` and `method` * property.
     * If an handler with the same path and method is added and the handler doesn't have
     * any documentation or the documentation cannot be accessed by reflection, the annotation will be used.
     * Currently this is just required for java method references.
     */
    fun activateAnnotationScanningFor(vararg packagePrefixes: String) = apply {
        packagePrefixesToScan.addAll(packagePrefixes)
    }

    fun modelConverterFactory(value: ModelConverterFactory) = apply { modelConverterFactory = value }

    fun toJsonMapper(value: ToJsonMapper) = apply { toJsonMapper = value }

    fun getFullDocumentationUrl(ctx: Context) = "${ctx.contextPath()}${path!!}"
}

fun OpenApiOptions(createInitialConfiguration: () -> OpenAPI) =
        OpenApiOptions(InitialConfigurationCreator(createInitialConfiguration))

@FunctionalInterface
interface DefaultDocumentation {
    fun apply(documentation: OpenApiDocumentation)
}

@FunctionalInterface
interface InitialConfigurationCreator {
    fun create(): OpenAPI
}

fun InitialConfigurationCreator(createInitialConfiguration: () -> OpenAPI) = object : InitialConfigurationCreator {
    override fun create() = createInitialConfiguration()
}

