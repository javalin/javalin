package io.javalin.community.ssl.util

import io.javalin.community.ssl.SslConfig
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * Helper class to create the requested [ServerConnector]s from the given config.
 */
class ConnectorFactory (private var config: SslConfig, private var sslContextFactory: SslContextFactory.Server?) {

    /**
     * Create and return an insecure connector to the server.
     *
     * @return The created [ServerConnector].
     */
    fun createInsecureConnector(server: Server?, httpConfiguration: HttpConfiguration): ServerConnector {
        val connector: ServerConnector

        //The http configuration object
        if(config.secure) httpConfiguration.securePort = config.securePort

        //The factory for HTTP/1.1 connections.
        val http11 = HttpConnectionFactory(httpConfiguration)
        connector = if (config.http2) {
            //The factory for HTTP/2 connections.
            val http2 = HTTP2CServerConnectionFactory(httpConfiguration)
            ServerConnector(server, http11, http2)
        } else {
            ServerConnector(server, http11)
        }
        connector.port = config.insecurePort

        config.host?.let { connector.host = it }
        config.configConnectors?.accept(connector)

        return connector
    }

    /**
     * Create and apply an SSL connector to the server.
     *
     * @return The created [ServerConnector].
     */
    fun createSecureConnector(server: Server?, httpConfiguration: HttpConfiguration): ServerConnector {
        val connector: ServerConnector

        httpConfiguration.addCustomizer(SecureRequestCustomizer(config.sniHostCheck))

        //The factory for HTTP/1.1 connections
        val http11 = HttpConnectionFactory(httpConfiguration)
        connector = if (config.http2) {
            //The factory for HTTP/2 connections.
            val http2 = HTTP2ServerConnectionFactory(httpConfiguration)
            // The ALPN ConnectionFactory.
            val alpn = ALPNServerConnectionFactory()
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(http11.protocol)
            val tlsHttp2 = SslConnectionFactory(sslContextFactory, alpn.protocol)
            ServerConnector(server, tlsHttp2, alpn, http2, http11)
        } else {
            val tls = SslConnectionFactory(sslContextFactory, http11.protocol)
            ServerConnector(server, tls, http11)
        }
        connector.port = config.securePort

        config.host?.let { connector.host = it }
        config.configConnectors?.accept(connector)

        return connector
    }
}
