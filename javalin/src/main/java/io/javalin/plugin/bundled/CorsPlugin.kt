package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.Header
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_EXPOSE_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.VARY
import io.javalin.http.HttpStatus
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.bundled.CorsPluginConfig.CorsRule
import io.javalin.plugin.bundled.CorsUtils.isValidOrigin
import io.javalin.plugin.bundled.CorsUtils.normalizeOrigin
import io.javalin.plugin.bundled.CorsUtils.originFulfillsWildcardRequirements
import io.javalin.plugin.bundled.CorsUtils.originsMatch
import io.javalin.plugin.bundled.CorsUtils.parseAsOriginParts
import io.javalin.plugin.createUserConfig
import java.util.*
import java.util.function.Consumer

class CorsPluginConfig : PluginConfiguration {
    internal val rules = mutableListOf<CorsRule>()

    fun addRule(rule: Consumer<CorsRule>): CorsPluginConfig = also {
        rules.add(CorsRule().also { rule.accept(it) })
    }

    class CorsRule {
        @JvmField var allowCredentials = false
        @JvmField var reflectClientOrigin = false
        @JvmField var defaultScheme = "https"
        @JvmField var path = "*"
        @JvmField var maxAge = -1

        private val allowedOrigins = mutableListOf<String>()
        private val headersToExpose = mutableListOf<String>()

        fun allowedOrigins(): List<String> =
            Collections.unmodifiableList(allowedOrigins)

        fun headersToExpose(): List<String> =
            Collections.unmodifiableList(headersToExpose)

        fun anyHost() {
            allowedOrigins.add("*")
        }

        fun allowHost(host: String, vararg others: String) {
            val origins = listOf(host) + others.toList()
            origins.map { CorsUtils.addSchemeIfMissing(it, defaultScheme) }.forEachIndexed { idx, it ->
                require(it != "null") { "Adding the string null as an allowed host is forbidden. Consider calling anyHost() instead" }
                require(isValidOrigin(it)) { "The given value '${origins[idx]}' could not be transformed into a valid origin" }
                val wildcardResult = originFulfillsWildcardRequirements(it)
                require(wildcardResult !is WildcardResult.ErrorState) {
                    when (wildcardResult) {
                        WildcardResult.ErrorState.TooManyWildcards -> "Too many wildcards detected inside '${origins[idx]}'. Only one at the start of the host is allowed!"
                        WildcardResult.ErrorState.WildcardNotAtTheStartOfTheHost -> "The wildcard must be at the start of the passed in host. The value '${origins[idx]}' violates this requirement!"
                        else -> throw IllegalStateException(
                            """This code path should never be hit.
                        |
                        |Please report it to the maintainers of Javalin as a GitHub issue at https://github.com/javalin/javalin/issues/new/choose""".trimMargin()
                        )
                    }
                }
                allowedOrigins.add(it)
            }
        }

        fun exposeHeader(header: String) {
            headersToExpose.add(header)
        }
    }
}

class CorsPlugin(config: Consumer<CorsPluginConfig>) : JavalinPlugin {

    open class Cors : PluginFactory<CorsPlugin, CorsPluginConfig> {
        override fun create(config: Consumer<CorsPluginConfig>): CorsPlugin = CorsPlugin(config)
    }

    companion object {
        object Cors : CorsPlugin.Cors()
    }

    private val corsConfig = config.createUserConfig(CorsPluginConfig())

    init {
        require(corsConfig.rules.isNotEmpty()) {
            "At least one cors config has to be provided. Use CorsContainer.add() to add one."
        }
    }

    override fun onStart(app: Javalin) {
        corsConfig.rules.forEach {
            applySingleConfig(app, it)
        }
    }

    private fun applySingleConfig(app: Javalin, cfg: CorsRule) {
        val origins = cfg.allowedOrigins()
        require(origins.isNotEmpty() || cfg.reflectClientOrigin) { "Origins cannot be empty if `reflectClientOrigin` is false." }
        require(origins.isEmpty() || !cfg.reflectClientOrigin) { "Cannot set `allowedOrigins` if `reflectClientOrigin` is true" }
        app.before(cfg.path) { ctx ->
            handleCors(ctx, cfg)
        }
        val validOptionStatusCodes = listOf(HttpStatus.NOT_FOUND, HttpStatus.METHOD_NOT_ALLOWED)
        app.after(cfg.path) { ctx ->
            if (ctx.method() == OPTIONS && ctx.status() in validOptionStatusCodes) { // CORS is enabled, so we return 200 for OPTIONS
                ctx.result("").status(200)
            }
        }
    }

    private fun handleCors(ctx: Context, cfg: CorsRule) {
        val clientOrigin = ctx.header(ORIGIN) ?: return

        if (!isValidOrigin(clientOrigin)) {
            return
        }

        if (ctx.method() == OPTIONS) {
            var requestedHeader = false // max-age is only needed if a header is requested

            ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also { headerValue ->
                ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, headerValue)
                requestedHeader = true
            }
            ctx.header(Header.ACCESS_CONTROL_REQUEST_METHOD)?.also { headerValue ->
                ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, headerValue)
                requestedHeader = true
            }
            if (requestedHeader && cfg.maxAge >= 0) {
                ctx.header(Header.ACCESS_CONTROL_MAX_AGE, cfg.maxAge.toString())
            }

        }

        val origins = cfg.allowedOrigins()
        val allowOriginValue: String = when {
            "*" in origins -> "*"
            clientOrigin == "null" -> return
            cfg.reflectClientOrigin -> clientOrigin
            matchOrigin(clientOrigin, origins) -> clientOrigin
            else -> {
                ctx.status(HttpStatus.BAD_REQUEST)
                return
            }
        }
        ctx.header(ACCESS_CONTROL_ALLOW_ORIGIN, allowOriginValue)
        ctx.header(VARY, ORIGIN)
        if (cfg.allowCredentials) {
            // should never be set to "false", but rather omitted
            ctx.header(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
        }

        val headersToExpose = cfg.headersToExpose()
        if (headersToExpose.isNotEmpty()) {
            ctx.header(ACCESS_CONTROL_EXPOSE_HEADERS, headersToExpose.joinToString(separator = ","))
        }
    }

    private fun matchOrigin(clientOrigin: String, origins: List<String>): Boolean {
        val clientOriginPart = parseAsOriginParts(normalizeOrigin(clientOrigin))
        val serverOriginParts = origins.map(::normalizeOrigin).map(::parseAsOriginParts)

        return serverOriginParts.any { originsMatch(clientOriginPart, it) }
    }

}
