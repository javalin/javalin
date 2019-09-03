package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.security.CoreRoles
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.util.CorsBeforeHandler
import io.javalin.http.util.CorsOptionsHandler
import io.javalin.plugin.openapi.OpenApiPlugin
import io.javalin.plugin.openapi.dsl.document
import io.javalin.plugin.openapi.dsl.documented

class CorsPlugin(private val origins: List<String>) : Plugin {

    init {
        require(origins.isNotEmpty()) { "Origins cannot be empty." }
    }

    companion object {
        @JvmStatic
        fun forOrigins(vararg origins: String) = CorsPlugin(origins.toList())

        @JvmStatic
        fun forAllOrigins() = CorsPlugin(listOf("*"))
    }

    override fun apply(app: Javalin) {
        val openApiPlugin = app.config.getPluginOrNull(OpenApiPlugin::class.java)
        val handler = if (openApiPlugin != null) {
            documented(document().operation {
                it.summary("CORS allowed for ${if (origins.contains("*")) "all" else "specific"} origins")
                it.description("This api allows CORS for the following origins: " + origins.joinToString(separator = ", "))
            }.result("200", null), CorsOptionsHandler())
        } else {
            CorsOptionsHandler()
        }

        app.before(CorsBeforeHandler(origins))
        app.options("*", handler, roles(CoreRoles.NO_WRAP))
    }
}
