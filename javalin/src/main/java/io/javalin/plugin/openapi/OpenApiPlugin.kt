package io.javalin.plugin.openapi

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.plugin.openapi.ui.ReDocRenderer
import io.javalin.plugin.openapi.ui.SwaggerRenderer

/**
 * Plugin for the the automatic generation of an open api schema.
 * The schema can be extracted with [JavalinOpenApi.createSchema].
 */
class OpenApiPlugin(private val options: OpenApiOptions) : Plugin, PluginLifecycleInit {

    lateinit var openApiHandler: OpenApiHandler

    override fun init(app: Javalin) {
        openApiHandler = OpenApiHandler(app, options)
    }

    override fun apply(app: Javalin) {
        Util.ensureDependencyPresent(OptionalDependency.SWAGGER_CORE)
        if (options.path == null && (options.swagger != null || options.reDoc != null)) {
            throw IllegalStateException("""
                Swagger or ReDoc is enabled, but there is no endpoint available for the OpenApi schema.
                Please use the `path` option of the OpenApiPlugin to set a schema endpoint.
            """.trimIndent().replace("\n", " "))
        }

        options.path?.let { path ->
            app.get(path, openApiHandler, options.roles)

            options.swagger?.let {
                Util.assertWebjarInstalled(OptionalDependency.SWAGGERUI)
                app.get(it.path, SwaggerRenderer(options), options.roles)
            }

            options.reDoc?.let {
                Util.assertWebjarInstalled(OptionalDependency.REDOC)
                app.get(it.path, ReDocRenderer(options), options.roles)
            }

            if (options.swagger != null || options.reDoc != null) {
                app.config.enableWebjars()
            }
        }
    }
}
