package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.http.Header.X_FORWARDED_PROTO
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.http.servlet.isLocalhost
import io.javalin.plugin.Plugin
import io.javalin.router.JavalinDefaultRouting.Companion.Default
import org.eclipse.jetty.server.ServerConnector
import java.util.function.Consumer

class SslRedirectPluginConfig {
    @JvmField var redirectOnLocalhost = false
    @JvmField var sslPort: Int? = null
}

/**
 * [SslRedirectPlugin] has to be the first registered plugin to properly handle all requests in 'before' handler.
 */
class SslRedirectPlugin(userConfig: Consumer<SslRedirectPluginConfig>? = null) : Plugin<SslRedirectPluginConfig>(userConfig, SslRedirectPluginConfig()) {

    override fun onStart(config: JavalinConfig) {
        config.router.mount {
            it.before { ctx ->
                if (!pluginConfig.redirectOnLocalhost && ctx.isLocalhost()) {
                    return@before
                }

                val xForwardedProto = ctx.header(X_FORWARDED_PROTO)

                if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
                    val urlWithHttps = ctx.fullUrl().replace("http", "https")

                    val urlWithHttpsAndPort = config.pvt.server
                        ?.takeIf { pluginConfig.sslPort != null }
                        ?.connectors
                        ?.filterIsInstance<ServerConnector>()
                        ?.firstOrNull { server -> urlWithHttps.contains(":${server.usedPort()}/") }
                        ?.let { server -> urlWithHttps.replaceFirst(":${server.usedPort()}/", ":${pluginConfig.sslPort}/") }
                        ?: urlWithHttps

                    ctx.redirect(
                        location = urlWithHttpsAndPort,
                        status = MOVED_PERMANENTLY
                    )
                }
            }
        }
    }

    // ServerConnector.port returns 0 if port is not set explicitly,
    // so we need to use localPort as a fallback value
    private fun ServerConnector.usedPort(): Int =
        port.takeIf { it != 0 } ?: localPort

}
