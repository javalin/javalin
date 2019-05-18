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
class OpenApiPlugin(
        private val options: OpenApiOptions
) : Plugin, PluginLifecycleInit {
    lateinit var openApiHandler: OpenApiHandler

    override fun init(app: Javalin) {
        openApiHandler = OpenApiHandler(app, options)
    }

    override fun apply(app: Javalin) {
        options.path?.let { path ->
            app.get(path, openApiHandler, options.roles)

            options.swagger?.let {
                Util.ensureDependencyPresent(OptionalDependency.SWAGGER_CORE)
                app.get(it.path, SwaggerRenderer(options))
            }

            options.reDoc?.let {
                app.get(it.path, ReDocRenderer(options))
            }

            if (options.swagger != null || options.reDoc != null) {
                app.config.enableWebjars()
            }
        }
    }
}
