package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.client.Request
import org.eclipse.jetty.client.Response
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.http2.client.HTTP2Client
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.io.ClientConnector
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.client.JettyUpgradeListener
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class TestWebSocketHttp2 {

    /** Creates an H2+TLS Javalin server and WS client, connects to [path], and asserts [expectedMessage]. */
    private fun test(
        path: String,
        expectedMessage: String,
        configBlock: (JavalinConfig) -> Unit
    ) {
        val app = Javalin.create { config ->
            configBlock(config)
            config.jetty.addConnector { server, httpConfig ->
                httpConfig.addCustomizer(SecureRequestCustomizer(false))
                val http11 = HttpConnectionFactory(httpConfig)
                val http2 = HTTP2ServerConnectionFactory(httpConfig)
                val alpn = ALPNServerConnectionFactory().apply { defaultProtocol = http11.protocol }
                val sslContextFactory = SslContextFactory.Server().apply {
                    keyStorePath = this::class.java.getResource("/keystore.jks")!!.toExternalForm()
                    keyStorePassword = "localhost"
                }
                val tls = SslConnectionFactory(sslContextFactory, alpn.protocol)
                ServerConnector(server, tls, alpn, http2, http11).apply { this.port = 0 }
            }
        }
        val wsClient = createH2WsClient()
        app.start(0)
        try {
            wsClient.start()
            val messageFuture = CompletableFuture<String>()
            val endpoint = object : Session.Listener.AbstractAutoDemanding() {
                override fun onWebSocketText(message: String) {
                    messageFuture.complete(message)
                }
            }
            val httpVersionFuture = CompletableFuture<HttpVersion>()
            val upgradeListener = object : JettyUpgradeListener {
                override fun onHandshakeResponse(request: Request, response: Response) {
                    httpVersionFuture.complete(response.version)
                }
            }
            wsClient.connect(endpoint, URI("wss://localhost:${app.port()}$path"), upgradeListener)
            assertThat(httpVersionFuture.get(5, TimeUnit.SECONDS)).isEqualTo(HttpVersion.HTTP_2)
            assertThat(messageFuture.get(5, TimeUnit.SECONDS)).isEqualTo(expectedMessage)
        } finally {
            wsClient.stop()
            app.stop()
        }
    }

    private fun createH2WsClient(): WebSocketClient {
        val clientSsl = SslContextFactory.Client(true)
        val clientConnector = ClientConnector().apply { sslContextFactory = clientSsl }
        val h2Client = HTTP2Client(clientConnector)
        val h2 = ClientConnectionFactoryOverHTTP2.HTTP2(h2Client)
        val h1 = HttpClientConnectionFactory.HTTP11()
        val transport = HttpClientTransportDynamic(clientConnector, h2, h1)
        return WebSocketClient(HttpClient(transport))
    }

    @Test
    fun `websocket works over http2 via direct jetty mapping`() = test("/ws/jetty", "hello from jetty") { config ->
        config.jetty.modifyServletContextHandler { context ->
            JettyWebSocketServletContainerInitializer.configure(context) { _, container ->
                container.addMapping("/ws/jetty") { _, _ ->
                    object : Session.Listener.AbstractAutoDemanding() {
                        override fun onWebSocketOpen(session: Session) {
                            session.sendText("hello from jetty", Callback.NOOP)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `websocket works over http2 via javalin routes`() = test("/ws/javalin", "hello from javalin") { config ->
        config.routes.ws("/ws/javalin") { ws ->
            ws.onConnect { ctx -> ctx.send("hello from javalin") }
        }
    }
}
