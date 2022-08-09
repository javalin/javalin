package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_EXPOSE_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.REFERER
import io.javalin.http.Header.VARY
import io.javalin.http.HttpStatus
import io.javalin.plugin.Plugin
import java.util.*
import java.util.function.Consumer

data class CorsPluginConfig(
    @JvmField var allowCredentials: Boolean = false,
    @JvmField var reflectClientOrigin: Boolean = false,
    private val allowedOrigins: MutableList<String> = mutableListOf(),
    private val headersToExpose: MutableList<String> = mutableListOf()
) {
    fun allowedOrigins(): List<String> = Collections.unmodifiableList(allowedOrigins)
    fun headersToExpose(): List<String> = Collections.unmodifiableList(headersToExpose)

    fun anyHost() {
        allowedOrigins.add("*")
    }

    fun allowHost(origin: String, vararg others: String) {
        val origins = listOf(origin) + others.toList()
        origins.forEach {
            allowedOrigins.add(it.removeSuffix("/"))
        }
    }

    fun exposeHeader(header: String) {
        headersToExpose.add(header)
    }
}

class CorsPlugin(userConfig: Consumer<CorsPluginConfig>) : Plugin {

    val cfg = CorsPluginConfig().also { userConfig.accept(it) }

    private val origins: List<String> = cfg.allowedOrigins()
    private val headersToExpose: List<String> = cfg.headersToExpose()

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
                ctx.header(
                    ACCESS_CONTROL_ALLOW_CREDENTIALS,
                    "true"
                ) // should never be set to "false", but rather omitted
            }

            if (headersToExpose.isNotEmpty()) {
                ctx.header(ACCESS_CONTROL_EXPOSE_HEADERS, headersToExpose.joinToString(separator = ","))
            }
        }
        app.after { ctx ->
            if (ctx.method() == OPTIONS && ctx.status() == HttpStatus.NOT_FOUND) { // CORS is enabled, so we return 200 for OPTIONS
                ctx.result("").status(200)
            }
        }
    }

}

