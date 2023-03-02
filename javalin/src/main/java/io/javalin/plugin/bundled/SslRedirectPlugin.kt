package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.Header.X_FORWARDED_PROTO
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.http.servlet.isLocalhost
import io.javalin.plugin.Plugin
import org.eclipse.jetty.server.ServerConnector

/**
 * [SslRedirectPlugin] has to be the first registered plugin to properly handle all requests in 'before' handler.
 */
class SslRedirectPlugin @JvmOverloads constructor(
    private val redirectOnLocalhost: Boolean = false,
    private val sslPort: Int? = null
) : Plugin {

    override fun apply(app: Javalin) {
        app.before { ctx ->
            if (!redirectOnLocalhost && ctx.isLocalhost()) {
                return@before
            }

            val xForwardedProto = ctx.header(X_FORWARDED_PROTO)

            if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
                val urlWithHttps = ctx.fullUrl().replace("http", "https")

                val urlWithHttpsAndPort = app.jettyServer()
                    ?.takeIf { sslPort != null }
                    ?.server()
                    ?.connectors
                    ?.filterIsInstance<ServerConnector>()
                    ?.firstOrNull { urlWithHttps.contains(":${it.usedPort()}/") }
                    ?.let { urlWithHttps.replaceFirst(":${it.usedPort()}/", ":${sslPort}/") }
                    ?: urlWithHttps

                ctx.redirect(
                    location = urlWithHttpsAndPort,
                    status = MOVED_PERMANENTLY
                )
            }
        }
    }

    // ServerConnector.port returns 0 if port is not set explicitly,
    // so we need to use localPort as a fallback value
    private fun ServerConnector.usedPort(): Int =
        port.takeIf { it != 0 }?: localPort

}
