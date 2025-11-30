package io.javalin.plugin.bundled

import java.net.URI
import java.util.*

internal object CorsUtils {

    /**
     * returns true if the [origin] is a valid origin according to [RFC6454](https://www.rfc-editor.org/rfc/rfc6454#section-7)
     *
     * __Notes:__
     * - We ignore list of origins
     */
    internal fun isValidOrigin(origin: String, client: Boolean = false): Boolean {
        if (origin.isEmpty()) {
            return false
        }
        if (origin == "null") {
            return true
        }
        val wildcardSnippet = "://*."
        val hasWildcard = wildcardSnippet in origin
        if (client && hasWildcard) {
            return false
        }
        val originWithoutWildcard = origin.replace(wildcardSnippet, "://")
        val originToAnalyze = if (hasWildcard) originWithoutWildcard else origin
        try {
            val uri = URI(originToAnalyze).parseServerAuthority()
            if (uri.path.isNullOrEmpty().not()) {
                return false
            }
            if (uri.userInfo.isNullOrEmpty().not()) {
                return false
            }
            if (uri.query.isNullOrEmpty().not()) {
                return false
            }
            if (uri.fragment.isNullOrEmpty().not()) {
                return false
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    internal fun parseAsOriginParts(origin: String): OriginParts {
        val wildcardSnippet = "://*."
        val hasWildcard = wildcardSnippet in origin
        val originWithoutWildcard = origin.replace(wildcardSnippet, "://")

        val uri: URI = URI(originWithoutWildcard).parseServerAuthority()

        require(uri.scheme != null) { "Scheme is required!" }

        val host: String = if (hasWildcard) {
            "*." + uri.host
        } else {
            uri.host
        }

        val port = when (uri.scheme to uri.port) {
            "https" to -1 -> 443
            "http" to -1 -> 80
            else -> uri.port
        }

        return OriginParts(uri.scheme, host, port)
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

internal sealed class WildcardResult {
    internal sealed class ErrorState : WildcardResult() {
        internal object TooManyWildcards : ErrorState()

        internal object WildcardNotAtTheStartOfTheHost : ErrorState()
    }

    internal object NoWildcardDetected : WildcardResult()

    internal object WildcardOkay : WildcardResult()
}

internal data class OriginParts(val scheme: String, val host: String, val port: Int)
