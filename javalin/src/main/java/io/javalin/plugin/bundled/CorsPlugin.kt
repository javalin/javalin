package io.javalin.plugin.bundled

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
import io.javalin.http.HttpStatus
import io.javalin.plugin.Plugin
import java.util.function.Consumer

data class CorsPluginConfig(
    @JvmField var allowCredentials: Boolean = false,
    @JvmField var reflectClientOrigin: Boolean = false,
    @JvmField var allowedOrigins: Collection<String> = listOf(),
)

class CorsPlugin(userConfig: Consumer<CorsPluginConfig>) : Plugin {

    val cfg = CorsPluginConfig().also { userConfig.accept(it) }

    private val origins: List<String> = cfg.allowedOrigins.map { it.removeSuffix("/") }

    override fun apply(app: Javalin) {
        require(origins.isNotEmpty() || cfg.reflectClientOrigin) { "Origins cannot be empty if `reflectClientOrigin` is false." }
        require(origins.isEmpty() || !cfg.reflectClientOrigin) { "Cannot set `allowedOrigins` if `reflectClientOrigin` is true" }
        app.before { ctx ->
            if (ctx.method() == OPTIONS) {
                ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also { headerValue ->
                    ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, headerValue)
                }
                ctx.header(ACCESS_CONTROL_REQUEST_METHOD)?.also { headerValue ->
                    ctx.header(ACCESS_CONTROL_ALLOW_METHODS, headerValue)
                }
            }
            val clientOrigin = ctx.header(ORIGIN) ?: ctx.header(REFERER) ?: return@before
            val allowOriginValue = when {
                "*" in origins -> "*"
                cfg.reflectClientOrigin -> clientOrigin
                clientOrigin in origins -> clientOrigin
                else -> null
            } ?: return@before
            ctx.header(ACCESS_CONTROL_ALLOW_ORIGIN, allowOriginValue)
            ctx.header(VARY, ORIGIN)
            if (cfg.allowCredentials) {
                ctx.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true") // should never be set to "false", but rather omitted
            }
        }
        app.after { ctx ->
            if (ctx.method() == OPTIONS && ctx.status() == HttpStatus.NOT_FOUND) { // CORS is enabled, so we return 200 for OPTIONS
                ctx.result("").status(200)
            }
        }
    }

}

