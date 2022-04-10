package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.util.JavalinLogger
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.plugin.openapi.ui.ReDocRenderer
import io.javalin.plugin.openapi.ui.SwaggerRenderer
import io.javalin.plugin.openapi.utils.OpenApiVersionUtil

/**
 * Plugin for the the automatic generation of an open api schema.
 * The schema can be extracted with [JavalinOpenApi.createSchema].
 */
class OpenApiPlugin(private vararg val options: OpenApiOptions) : Plugin, PluginLifecycleInit {

    init {
        if (OpenApiVersionUtil.logWarnings && OpenApiVersionUtil.warning != null) {
            JavalinLogger.warn("${OpenApiVersionUtil.warning} - the OpenAPI plugin will not work properly. " +
                    "Please visit https://github.com/tipsy/javalin/issues/1193 if you want to help fix this issue.")
            JavalinLogger.warn("You can disable this warning by doing `OpenApiVersionUtil.logWarnings = false`")
        }
    }

    lateinit var openApiHandler: OpenApiHandler

    private val handlerMap = mutableMapOf<String, OpenApiHandler>()

    override fun init(app: Javalin) {
        if (options.isEmpty()) {
            throw IllegalArgumentException("The OpenApiPlugin requires at least one set of Options")
        }
        openApiHandler = OpenApiHandler(app, options.first())
        options.forEach {
            it.path?.let { path ->
                handlerMap.putIfAbsent(path, OpenApiHandler(app, it))
            }
        }
    }

    override fun apply(app: Javalin) {
        Util.ensureDependencyPresent(OptionalDependency.SWAGGER_CORE)
        options.forEach { options ->
            if (options.path == null && (options.swagger != null || options.reDoc != null)) {
                throw IllegalStateException("""
                Swagger or ReDoc is enabled, but there is no endpoint available for the OpenApi schema.
                Please use the `path` option of the OpenApiPlugin to set a schema endpoint.
            """.trimIndent().replace("\n", " "))
            }

            options.path?.let { path ->
                app.get(path, handlerMap[path]!!, *options.roles.toTypedArray())

                options.swagger?.let {
                    Util.assertWebjarInstalled(OptionalDependency.SWAGGERUI)
                    app.get(it.path, SwaggerRenderer(options), *options.roles.toTypedArray())
                }

                options.reDoc?.let {
                    Util.assertWebjarInstalled(OptionalDependency.REDOC)
                    app.get(it.path, ReDocRenderer(options), *options.roles.toTypedArray())
                }

                if (options.swagger != null || options.reDoc != null) {
                    app._conf.enableWebjars()
                }
            }

        }

    }
}
