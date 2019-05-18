package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.security.CoreRoles
import io.javalin.core.security.SecurityUtil.roles
import io.javalin.http.util.CorsBeforeHandler
import io.javalin.http.util.CorsOptionsHandler

class CorsPlugin(private val origins: List<String>) : Plugin {
    init {
        if (origins.isEmpty()) {
            throw IllegalArgumentException("Origins cannot be empty.")
        }
    }

    companion object {
        @JvmStatic
        fun forOrigins(vararg origins: String) = CorsPlugin(origins.toList())

        @JvmStatic
        fun forAllOrigins() = CorsPlugin(listOf("*"))
    }

    override fun apply(app: Javalin) {
        if (origins.isEmpty()) {
            return
        }

        app.before(CorsBeforeHandler(origins))
        app.options("*", CorsOptionsHandler(), roles(CoreRoles.NO_WRAP))
    }
}
