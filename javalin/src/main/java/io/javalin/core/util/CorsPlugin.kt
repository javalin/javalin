package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.http.util.CorsBeforeHandler

class CorsPlugin(private val origins: List<String>) : Plugin {

    override fun apply(app: Javalin) {
        if (origins.isEmpty()) {
            throw IllegalArgumentException("Origins cannot be empty.")
        }
        app.before(CorsBeforeHandler(origins))
    }

    companion object {
        @JvmStatic
        fun forOrigins(vararg origins: String) = CorsPlugin(origins.toList())

        @JvmStatic
        fun forAllOrigins() = CorsPlugin(listOf("*"))
    }

}
