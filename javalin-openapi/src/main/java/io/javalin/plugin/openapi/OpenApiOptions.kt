package io.javalin.plugin.openapi

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.security.Role
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.plugin.json.ToJsonMapper
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.dsl.documented
import io.javalin.plugin.openapi.jackson.JacksonModelConverterFactory
import io.javalin.plugin.openapi.jackson.JacksonToJsonMapper
import io.javalin.plugin.openapi.ui.ReDocOptions
import io.javalin.plugin.openapi.ui.SwaggerOptions
import io.javalin.plugin.openapi.utils.LazyDefaultValue
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
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
     * The default jackson mapper used, for "modelConverterFactory" and "toJsonMapper" if not overridden.
     */
    var jacksonMapper: ObjectMapper by LazyDefaultValue { JacksonToJsonMapper.defaultObjectMapper }
    /**
     * Creates a model converter, which converts a class to an open api schema.
     * Defaults to the jackson converter.
     */
    var modelConverterFactory: ModelConverterFactory by LazyDefaultValue { JacksonModelConverterFactory(jacksonMapper) }
    /**
     * The json mapper for creating the object api schema json. This is separated from
     * the default JavalinJson mappers.
     */
    var toJsonMapper: ToJsonMapper by LazyDefaultValue { JacksonToJsonMapper(jacksonMapper) }
    /**
     * A list of package prefixes to scan for annotations.
     */
    var packagePrefixesToScan = mutableSetOf<String>()
    /**
     * Manual set the documentation of specific paths
     */
    var overriddenDocumentation: MutableList<HandlerMetaInfo> = mutableListOf()

    /**
     * A list of paths to ignore in documentation
     */
    var ignoredPaths: MutableList<Pair<String, List<HttpMethod>>> = mutableListOf()

    /**
     * Validate the generated schema with the swagger parser
     * (prints warnings if schema is invalid)
     */
    var validateSchema: Boolean = false

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

    fun examples(examples: Map<Class<*>, Map<String, Example>>) {
        openApiExamples = examples.mapValues { it.value.toMutableMap() }.toMutableMap()
    }

    @JvmSynthetic
    inline fun <reified T : Any> addExample(name: String, example: Example) = addExample(T::class.java, name, example)

    fun <T> addExample(clazz: Class<T>, name: String, example: Example) {
        openApiExamples.computeIfAbsent(clazz) { mutableMapOf() }[name] = example
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

    fun jacksonMapper(value: ObjectMapper) = apply { jacksonMapper = value }

    fun modelConverterFactory(value: ModelConverterFactory) = apply { modelConverterFactory = value }

    fun toJsonMapper(value: ToJsonMapper) = apply { toJsonMapper = value }

    fun getFullDocumentationUrl(ctx: Context) = "${ctx.contextPath()}${path!!}"

    fun setDocumentation(path: String, method: HttpMethod, documentation: OpenApiDocumentation) = apply {
        overriddenDocumentation.add(HandlerMetaInfo(HandlerType.valueOf(method.name), path, documented(documentation, Handler { }), emptySet()))
    }

    fun validateSchema(validate: Boolean = true) = apply { validateSchema = validate }

    fun ignorePath(path: String, vararg httpMethod: HttpMethod) = apply {
        ignoredPaths.add(Pair(path, httpMethod.asList().ifEmpty { HttpMethod.values().asList() }))
    }
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

internal var openApiExamples = mutableMapOf<Class<*>, MutableMap<String, Example>>()
