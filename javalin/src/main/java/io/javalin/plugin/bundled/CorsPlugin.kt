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
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.plugin.bundled.CorsUtils.isValidOrigin
import io.javalin.plugin.bundled.CorsUtils.normalizeOrigin
import io.javalin.plugin.bundled.CorsUtils.originsMatch
import io.javalin.plugin.bundled.CorsUtils.parseAsOriginParts
import java.util.*
import java.util.function.Consumer

data class CorsPluginConfig(
    @JvmField var allowCredentials: Boolean = false,
    @JvmField var reflectClientOrigin: Boolean = false,
    @JvmField var defaultScheme: String = "https",
    private val allowedOrigins: MutableList<String> = mutableListOf(),
    private val headersToExpose: MutableList<String> = mutableListOf()
) {
    fun allowedOrigins(): List<String> = Collections.unmodifiableList(allowedOrigins)
    fun headersToExpose(): List<String> = Collections.unmodifiableList(headersToExpose)

    fun anyHost() {
        allowedOrigins.add("*")
    }

    fun allowHost(host: String, vararg others: String) {
        val origins = listOf(host) + others.toList()
        origins.map { CorsUtils.addSchemeIfMissing(it, defaultScheme) }.forEachIndexed { idx, it ->
            require(it != "null") { "Adding the string null as an allowed host is forbidden. Consider calling anyHost() instead" }
            require(isValidOrigin(it)) { "The given value '${origins[idx]}' could not be transformed into a valid origin" }
            allowedOrigins.add(it)
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
        val clientOrigin = ctx.header(ORIGIN) ?: return

        if (!isValidOrigin(clientOrigin)) {
            return
        }

        if (ctx.method() == OPTIONS) {
            ctx.header(ACCESS_CONTROL_REQUEST_HEADERS)?.also { headerValue ->
                ctx.header(ACCESS_CONTROL_ALLOW_HEADERS, headerValue)
            }
            ctx.header(Header.ACCESS_CONTROL_REQUEST_METHOD)?.also { headerValue ->
                ctx.header(Header.ACCESS_CONTROL_ALLOW_METHODS, headerValue)
            }
        }

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

        if (headersToExpose.isNotEmpty()) {
            ctx.header(ACCESS_CONTROL_EXPOSE_HEADERS, headersToExpose.joinToString(separator = ","))
        }
    }

    private fun matchOrigin(clientOrigin: String, origins: List<String>): Boolean {
        val clientOriginPart = parseAsOriginParts(normalizeOrigin(clientOrigin))
        val serverOriginParts = origins.map(::normalizeOrigin).map(::parseAsOriginParts)
        for (serverOriginPart in serverOriginParts) {
            if (originsMatch(clientOriginPart, serverOriginPart)) {
                return true
            }
        }
        return false
    }
}

internal object CorsUtils {
    /**
     * validates a given scheme against [the RFC3986 definition](https://www.rfc-editor.org/rfc/rfc3986#section-3.1)
     */
    internal fun isSchemeValid(proto: CharSequence) = proto.isNotEmpty() && proto[0].isLetter() && proto.all { ch ->
        ch.isLetter() || ch.isAsciiDigit() || ch == '-' || ch == '+' || ch == '.'
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

        // only slashed that are allowed are the delimiter slashes
        if (origin.count { it == '/' } != 2) return false

        // if a port is specified is must consist of only digits
        val portResult = extractPort(origin)
        if (portResult is PortResult.ErrorState) return false

        return true
    }

    /**
     * Tries to extract a port from a given origin.
     * If the origin does not contain a scheme [PortResult.ErrorState.InvalidOrigin] is returned.
     * If no port is specified [PortResult.NoPortSpecified] is returned.
     * If the port value contains non digit characters [PortResult.ErrorState.InvalidPort] is returned.
     * Otherwise [PortResult.PortSpecified] with the extracted [PortResult.PortSpecified.port] value is returned.
     *
     * __Notes:__
     * - Should only be called on an origin with a valid scheme, see [isSchemeValid]
     */
    internal fun extractPort(origin: String): PortResult {
        if ("://" !in origin) return PortResult.ErrorState.InvalidOrigin
        val possiblePortIndex = origin.lastIndexOf(':')
        val colonAfterSchemeIndex = origin.indexOf(':')
        val hasPort = possiblePortIndex != colonAfterSchemeIndex
        if (!hasPort) {
            return PortResult.NoPortSpecified
        }
        val possiblePortDigits = origin.subSequence(possiblePortIndex + 1, origin.length).toString()
        if (possiblePortDigits.any { !it.isAsciiDigit() }) {
            return PortResult.ErrorState.InvalidPort
        }
        return PortResult.PortSpecified(possiblePortDigits.toInt(radix = 10))
    }

    /**
     * Tries to extract a port from a given origin, falling back to https / http scheme defaults if possible.
     */
    internal fun extractPortOrSchemeDefault(origin: String): PortResult {
        val portResult = extractPort(origin)
        if (portResult !is PortResult.NoPortSpecified) {
            return portResult
        }
        return when {
            origin.startsWith("https://", ignoreCase = true) -> PortResult.PortSpecified(443, fromSchemeDefault = true)
            origin.startsWith("http://", ignoreCase = true) -> PortResult.PortSpecified(80, fromSchemeDefault = true)
            else -> portResult
        }
    }

    internal fun addSchemeIfMissing(host: String, defaultScheme: String): String {
        // do not add a scheme to special values
        if (host == "*" || host == "null") {
            return host
        }
        val hostWithScheme = if ("://" in host) {
            host
        } else {
            "$defaultScheme://$host"
        }

        return hostWithScheme.lowercase(Locale.ROOT).removeSuffix("/")
    }

    internal fun normalizeOrigin(origin: String): String {
        val portResult = extractPortOrSchemeDefault(origin)
        if (portResult is PortResult.PortSpecified && portResult.fromSchemeDefault) {
            return "$origin:${portResult.port}"
        }
        return origin
    }

    internal fun parseAsOriginParts(origin: String): OriginParts {
        val schemeAndHostDelimiter =
            origin.indexOf("://").also { require(it > 0) { "scheme delimiter :// must exist" } }
        val scheme: String =
            origin.subSequence(0, schemeAndHostDelimiter).toString().also { require(isSchemeValid(it)) { "specified scheme is not valid" } }
        val port = (extractPort(origin) as? PortResult.PortSpecified)?.port ?: throw IllegalArgumentException("explicit port is required")
        val host = origin.subSequence(schemeAndHostDelimiter + 3, origin.lastIndexOf(':')).toString()

        val reconstructedOrigin = "$scheme://$host:$port"
        require(reconstructedOrigin == origin) {
            """Parsing failed!
            |reconstructedOrigin '$reconstructedOrigin' did not match the original origin '$origin'.
            |This is a critical error and should never happen!
            |
            |Please report it as a GitHub issue at https://github.com/javalin/javalin/issues/new/choose with the exact error message!
            """.trimMargin()
        }

        return OriginParts(scheme, host, port)
    }

    internal fun originsMatch(clientOrigin: OriginParts, serverOrigin: OriginParts): Boolean {
        if (clientOrigin == serverOrigin) {
            return true
        }
        if (clientOrigin.scheme != serverOrigin.scheme) {
            return false
        }

        if (clientOrigin.port != serverOrigin.port) {
            return false
        }

        // server host value could be something like `*.example.com`
        // if the client sends a.example.com it should match
        if (!serverOrigin.host.startsWith("*.")) {
            return false
        }
        val serverHostBase = serverOrigin.host.removePrefix("*.")
        val clientHostBase = clientOrigin.host.split('.', limit = 2)[1]

        return serverHostBase == clientHostBase
    }
}

internal sealed class PortResult {
    internal sealed class ErrorState : PortResult() {
        internal object InvalidOrigin : ErrorState()
        internal object InvalidPort : ErrorState()
    }

    internal object NoPortSpecified : PortResult()
    internal data class PortSpecified(val port: Int, val fromSchemeDefault: Boolean = false) : PortResult()
}

internal data class OriginParts(val scheme: String, val host: String, val port: Int)

/**
 * Returns true if and only if the character is one of the ascii digits (0 - 9).
 *
 * The existing Char.isDigit() also accepts other digit symbols such as "MATHEMATICAL BOLD DIGIT ZERO" as it accepts any
 * character with CharCategory.DECIMAL_DIGIT_NUMBER
 * RFC2234 defines digits as 0 - 9 only.
 *
 * Ref: https://www.rfc-editor.org/rfc/rfc2234#section-3.4
 * Ref: https://www.fileformat.info/info/unicode/category/Nd/list.htm
 */
private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'
