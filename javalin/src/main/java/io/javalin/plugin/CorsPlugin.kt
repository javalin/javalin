package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.REFERER
import io.javalin.http.Header.VARY
import java.util.function.Consumer

data class CorsPluginConfig(
    @JvmField var allowCredentials: Boolean = false,
    @JvmField var allowAllOrigins: Boolean = false,
    @JvmField var allowedOrigins: Collection<String> = listOf(),
)

class CorsPlugin(userConfig: Consumer<CorsPluginConfig>) : Plugin {

    val cfg = CorsPluginConfig().also { userConfig.accept(it) }

    private val origins: List<String> = cfg.allowedOrigins.map { it.removeSuffix("/") }

    override fun apply(app: Javalin) {
        require(origins.isNotEmpty() || cfg.allowAllOrigins) { "Origins cannot be empty if `allowAllOrigins` is false." }
        require(origins.isEmpty() || !cfg.allowAllOrigins) { "Cannot set `allowedOrigins` if `allowAllOrigins` is true" }
        app.before { ctx ->
            if (ctx.method() == OPTIONS) {
                ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also { headerValue ->
                    ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, headerValue)
                }
                ctx.header(ACCESS_CONTROL_REQUEST_METHOD)?.also { headerValue ->
                    ctx.header(ACCESS_CONTROL_ALLOW_METHODS, headerValue)
                }
            }
            val requestOrigin = ctx.header(ORIGIN) ?: ctx.header(REFERER) ?: return@before
            if (!cfg.allowAllOrigins && requestOrigin !in origins) return@before
            ctx.header(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
            ctx.header(VARY, ORIGIN)
            if (cfg.allowCredentials) {
                ctx.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") // should never be set to "false", but rather omitted
            }
        }
    }

}

