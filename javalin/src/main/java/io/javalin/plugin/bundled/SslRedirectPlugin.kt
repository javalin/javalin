package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.http.Header.X_FORWARDED_PROTO
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.http.servlet.isLocalhost
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginFactory
import org.eclipse.jetty.server.ServerConnector
import java.util.function.Consumer

object SslRedirectPluginFactory : PluginFactory<SslRedirectPlugin, SslRedirectPluginConfig> {
    override fun create(config: Consumer<SslRedirectPluginConfig>): SslRedirectPlugin {
        return SslRedirectPlugin(config)
    }
}

class SslRedirectPluginConfig {
    @JvmField var redirectOnLocalhost = false
    @JvmField var sslPort: Int? = null
}

/**
 * [SslRedirectPlugin] has to be the first registered plugin to properly handle all requests in 'before' handler.
 */
class SslRedirectPlugin(config: Consumer<SslRedirectPluginConfig> = Consumer {}) : JavalinPlugin {

    companion object {
        @JvmStatic val FACTORY = SslRedirectPluginFactory
    }

    private val config = SslRedirectPluginConfig().apply { config.accept(this) }

    override fun onStart(app: Javalin) {
        app.before { ctx ->
            if (!config.redirectOnLocalhost && ctx.isLocalhost()) {
                return@before
            }

            val xForwardedProto = ctx.header(X_FORWARDED_PROTO)

            if (xForwardedProto == "http" || (xForwardedProto == null && ctx.scheme() == "http")) {
                val urlWithHttps = ctx.fullUrl().replace("http", "https")

                val urlWithHttpsAndPort = app.jettyServer()
                    ?.takeIf { config.sslPort != null }
                    ?.server()
                    ?.connectors
                    ?.filterIsInstance<ServerConnector>()
                    ?.firstOrNull { urlWithHttps.contains(":${it.usedPort()}/") }
                    ?.let { urlWithHttps.replaceFirst(":${it.usedPort()}/", ":${config.sslPort}/") }
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
        port.takeIf { it != 0 } ?: localPort

}
