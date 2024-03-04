package io.javalin.plugin.bundled

import java.net.URI
import java.util.*

internal object CorsUtils {
    /**
     * validates a given scheme against [the RFC3986 definition](https://www.rfc-editor.org/rfc/rfc3986#section-3.1)
     */
    internal fun isSchemeValid(scheme: CharSequence) = scheme.isNotEmpty() && scheme.first().isLetter() && scheme.all {
        it.isLetter() || it.isAsciiDigit() || it == '-' || it == '+' || it == '.'
    }

    /**
     * returns true if the [origin] is a valid origin according to [RFC6454](https://www.rfc-editor.org/rfc/rfc6454#section-7)
     *
     * __Notes:__
     * - We ignore list of origins
     */
    internal fun isValidOrigin(origin: String): Boolean {
        val schemeAndHostDelimiter = origin.indexOf("://")
        val portResult = extractPort(origin)
        return when {
            origin.isEmpty() -> false
            origin == "null" -> true
            // query strings are not a valid part of an origin
            "?" in origin -> false
            // fragments are not a valid part of an origin
            "#" in origin -> false
            // schemes are a required part of an origin
            schemeAndHostDelimiter <= 0 -> false
            !isSchemeValid(origin.subSequence(0, schemeAndHostDelimiter)) -> false
            // only slashes that are allowed are the delimiter slashes
            origin.count { it == '/' } != 2 -> false
            // if a port is specified is must consist of only digits
            portResult is PortResult.ErrorState -> false
            else -> true
        }
    }

    internal fun isValidOriginJdk(origin: String): Boolean {
        if (origin.isEmpty()) {
            return false
        }
        if (origin == "null") {
            return true
        }
        try {
            val uri = URI(origin).parseServerAuthority()
            if (uri.path.isNullOrEmpty().not()) {
                return false
            }
            if (uri.query.isNullOrEmpty().not()) {
                return false
            }
            if (uri.fragment.isNullOrEmpty().not()) {
                return false
            }
            return true
        } catch (e: Exception) {
            return false
        }
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
        return when {
            portResult is PortResult.NoPortSpecified && origin.startsWith("https://", ignoreCase = true) ->
                PortResult.PortSpecified(443, fromSchemeDefault = true)

            portResult is PortResult.NoPortSpecified && origin.startsWith("http://", ignoreCase = true) ->
                PortResult.PortSpecified(80, fromSchemeDefault = true)

            else -> portResult
        }
    }

    internal fun addSchemeIfMissing(host: String, defaultScheme: String): String {
        val hostWithScheme = when {
            // do not add a scheme to special values
            host == "*" -> host
            host == "null" -> host
            "://" in host -> host
            else -> "$defaultScheme://$host"
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
        val scheme: String = origin.subSequence(0, schemeAndHostDelimiter).toString()
            .also { require(isSchemeValid(it)) { "specified scheme is not valid" } }
        val port = (extractPort(origin) as? PortResult.PortSpecified)?.port
            ?: throw IllegalArgumentException("explicit port is required")
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
        // the above lines imply a requirement of at least one dot in the origin header sent by the client
        if ('.' !in clientOrigin.host) {
            return false
        }
        val serverHostBase = serverOrigin.host.removePrefix("*.")
        val clientHostBase = clientOrigin.host.split('.', limit = 2)[1]

        return serverHostBase == clientHostBase
    }

    internal fun originFulfillsWildcardRequirements(origin: String): WildcardResult {
        return when (origin.count { it == '*' }) {
            0 -> WildcardResult.NoWildcardDetected
            1 -> if ("://*." !in origin) {
                WildcardResult.ErrorState.WildcardNotAtTheStartOfTheHost
            } else {
                WildcardResult.WildcardOkay
            }

            else -> WildcardResult.ErrorState.TooManyWildcards
        }
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

internal sealed class WildcardResult {
    internal sealed class ErrorState : WildcardResult() {
        internal object TooManyWildcards : ErrorState()

        internal object WildcardNotAtTheStartOfTheHost : ErrorState()
    }

    internal object NoWildcardDetected : WildcardResult()

    internal object WildcardOkay : WildcardResult()
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
