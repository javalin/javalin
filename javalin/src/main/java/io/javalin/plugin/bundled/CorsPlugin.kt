package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HandlerType.OPTIONS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_EXPOSE_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.VARY
import io.javalin.http.HttpStatus
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.plugin.bundled.CorsUtils.isValidOrigin
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

class CorsPlugin(userConfig: Consumer<CorsPluginConfig>) : Plugin, PluginLifecycleInit {

    val cfg = CorsPluginConfig().also { userConfig.accept(it) }

    private val origins: List<String> = cfg.allowedOrigins()
    private val headersToExpose: List<String> = cfg.headersToExpose()

    override fun init(app: Javalin) {
        app.cfg.plugins.enableHttpAllowedMethodsOnRoutes()
    }

    override fun apply(app: Javalin) {
        require(origins.isNotEmpty() || cfg.reflectClientOrigin) { "Origins cannot be empty if `reflectClientOrigin` is false." }
        require(origins.isEmpty() || !cfg.reflectClientOrigin) { "Cannot set `allowedOrigins` if `reflectClientOrigin` is true" }
        app.before { ctx ->
            handleCors(ctx)
        }
        app.after { ctx ->
            if (ctx.method() == OPTIONS && ctx.status() == HttpStatus.NOT_FOUND) { // CORS is enabled, so we return 200 for OPTIONS
                ctx.result("").status(200)
            }
        }
    }

    private fun handleCors(ctx: Context) {
        val clientOrigin = ctx.header(ORIGIN) ?: run {
            ctx.status(HttpStatus.BAD_REQUEST)
            return
        }

        if (!isValidOrigin(clientOrigin)) {
            return
        }

        if (ctx.method() == OPTIONS) {
            ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also { headerValue ->
                ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, headerValue)
            }
        }

        val allowOriginValue: String = when {
            "*" in origins -> "*"
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

        if (headersToExpose.isNotEmpty()) {
            ctx.header(ACCESS_CONTROL_EXPOSE_HEADERS, headersToExpose.joinToString(separator = ","))
        }
    }

    private fun matchOrigin(clientOrigin: String, origins: List<String>): Boolean {
        if (clientOrigin in origins) {
            return true
        }

        return false
    }
}

internal object CorsUtils {
    /**
     * validates a given scheme against [the RFC3986 definition](https://www.rfc-editor.org/rfc/rfc3986#section-3.1)
     */
    internal fun isSchemeValid(proto: CharSequence) = proto.isNotEmpty() && proto[0].isLetter() && proto.all { ch ->
        ch.isLetter() || ch.isDigit() || ch == '-' || ch == '+' || ch == '.'
    }

    /**
     * returns true if the [origin] is a valid origin according to [RFC6454](https://www.rfc-editor.org/rfc/rfc6454#section-7)
     *
     * __Notes:__
     * - We ignore list of origins
     */
    internal fun isValidOrigin(origin: String): Boolean {
        if (origin.isEmpty()) return false
        if (origin == "null") return true
        // query strings are not a valid part of an origin
        if ("?" in origin) return false
        // schemes are a required part of an origin
        val schemeAndHostDelimiter = origin.indexOf("://")

        if (schemeAndHostDelimiter <= 0) return false
        if (!isSchemeValid(origin.subSequence(0, schemeAndHostDelimiter))) return false

        // if a port is specified is must consist of only digits
        val portResult = extractPort(origin)
        if (portResult is PortResult.ErrorState) return false

        return true
    }

    /**
     * Tries to extract a port from a given origin.
     * If the origin does not contain a colon [PortResult.ErrorState.InvalidOrigin] is returned.
     * If no port is specified [PortResult.NoPortSpecified] is returned.
     * If the port value contains non digit characters [PortResult.ErrorState.InvalidPort] is returned.
     * Otherwise [PortResult.PortSpecified] with the extracted [PortResult.PortSpecified.port] value is returned.
     *
     * __Notes:__
     * - Should only be called on an origin with a valid scheme, see [isSchemeValid]
     */
    internal fun extractPort(origin: String): PortResult {
        if (':' !in origin) return PortResult.ErrorState.InvalidOrigin
        val possiblePortIndex = origin.lastIndexOf(':')
        val colonAfterSchemeIndex = origin.indexOf(':')
        val hasPort = possiblePortIndex != colonAfterSchemeIndex
        if (!hasPort) {
            return PortResult.NoPortSpecified
        }
        val possiblePortDigits = origin.subSequence(possiblePortIndex + 1, origin.length).toString()
        // Char.isDigit() also accepts other digit symbols such as "MATHEMATICAL BOLD DIGIT ZERO" as it accepts any
        // character with CharCategory.DECIMAL_DIGIT_NUMBER
        // RFC2234 defines digits as 0 - 9 only.
        // Ref: https://www.rfc-editor.org/rfc/rfc2234#section-3.4
        // Ref: https://www.fileformat.info/info/unicode/category/Nd/list.htm
        if (possiblePortDigits.any { it !in '0'..'9' }) {
            return PortResult.ErrorState.InvalidPort
        }
        return PortResult.PortSpecified(possiblePortDigits.toInt(radix = 10))
    }
}

internal sealed class PortResult {
    internal sealed class ErrorState : PortResult() {
        internal object InvalidOrigin : ErrorState()
        internal object InvalidPort : ErrorState()
    }

    internal object NoPortSpecified : PortResult()
    internal data class PortSpecified(val port: Int) : PortResult()
}
