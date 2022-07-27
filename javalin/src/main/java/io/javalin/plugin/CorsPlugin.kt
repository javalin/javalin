package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.Handler
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

class CorsPlugin(private val origins: List<String>) : Plugin {

    override fun apply(app: Javalin) {
        if (origins.isEmpty()) {
            throw IllegalArgumentException("Origins cannot be empty.")
        }

        app.before(CorsBeforeHandler(origins))
    }

    companion object {
        @JvmStatic
        fun forOrigins(vararg origins: String): CorsPlugin = CorsPlugin(origins.toList())

        @JvmStatic
        fun forAllOrigins(): CorsPlugin = CorsPlugin(listOf("*"))
    }

}

class CorsBeforeHandler(origins: List<String>) : Handler {

    private val origins: List<String> = origins.map { it.removeSuffix("/") }

    override fun handle(ctx: Context) {
        val originHeader = ctx.header(ORIGIN) ?: ctx.header(REFERER)

        if (originHeader != null) {
            origins
                .firstOrNull { it == "*" || originHeader == it }
                ?.run {
                    ctx.header(ACCESS_CONTROL_ALLOW_ORIGIN, originHeader)
                    ctx.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                    ctx.header(VARY, ORIGIN)
                }
        }

        if (ctx.method() == OPTIONS) {
            ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also {
                ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, it)
            }
            ctx.header(ACCESS_CONTROL_REQUEST_METHOD)?.also {
                ctx.header(ACCESS_CONTROL_ALLOW_METHODS, it)
            }
        }
    }

}
