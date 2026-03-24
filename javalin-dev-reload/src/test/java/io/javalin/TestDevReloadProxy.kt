@file:Suppress("DEPRECATION")

package io.javalin

import io.javalin.plugin.bundled.DevReloadProxy
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.FilterHolder
import jakarta.servlet.DispatcherType
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URI
import java.util.EnumSet

class TestDevReloadProxy {

    private fun findFreePort() = ServerSocket(0).use { it.localPort }

    /** Start a minimal Jetty server with DevReloadProxy as its only filter. */
    private fun startProxyServer(proxy: DevReloadProxy, port: Int): Server {
        val server = Server(port)
        val handler = ServletContextHandler()
        handler.addFilter(FilterHolder(proxy), "/*", EnumSet.of(DispatcherType.REQUEST))
        server.handler = handler
        server.start()
        return server
    }

    @Test
    fun `returns 503 when no target port set`() {
        val proxy = DevReloadProxy()
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            val conn = URI("http://localhost:$proxyPort/hello").toURL().openConnection() as HttpURLConnection
            assertThat(conn.responseCode).isEqualTo(503)
            val body = conn.errorStream.bufferedReader().readText()
            assertThat(body).contains("Recompiling")
        } finally {
            server.stop()
        }
    }

    @Test
    fun `forwards request to target when port is set`() {
        val backendPort = findFreePort()
        val backend = Javalin.create { it.routes.get("/hello") { ctx -> ctx.result("backend-response") } }.start(backendPort)
        val proxy = DevReloadProxy()
        proxy.targetPort = backendPort
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            val conn = URI("http://localhost:$proxyPort/hello").toURL().openConnection() as HttpURLConnection
            assertThat(conn.responseCode).isEqualTo(200)
            assertThat(conn.inputStream.bufferedReader().readText()).isEqualTo("backend-response")
        } finally {
            server.stop()
            backend.stop()
        }
    }

    @Test
    fun `returns 503 when target port becomes unavailable`() {
        val proxy = DevReloadProxy()
        proxy.targetPort = findFreePort() // port with nothing listening
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            val conn = URI("http://localhost:$proxyPort/hello").toURL().openConnection() as HttpURLConnection
            assertThat(conn.responseCode).isEqualTo(503)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `proxy can switch targets`() {
        val backend1Port = findFreePort()
        val backend1 = Javalin.create { it.routes.get("/hello") { ctx -> ctx.result("v1") } }.start(backend1Port)
        val backend2Port = findFreePort()
        val backend2 = Javalin.create { it.routes.get("/hello") { ctx -> ctx.result("v2") } }.start(backend2Port)

        val proxy = DevReloadProxy()
        proxy.targetPort = backend1Port
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            var conn = URI("http://localhost:$proxyPort/hello").toURL().openConnection() as HttpURLConnection
            assertThat(conn.inputStream.bufferedReader().readText()).isEqualTo("v1")

            proxy.targetPort = backend2Port
            conn = URI("http://localhost:$proxyPort/hello").toURL().openConnection() as HttpURLConnection
            assertThat(conn.inputStream.bufferedReader().readText()).isEqualTo("v2")
        } finally {
            server.stop()
            backend1.stop()
            backend2.stop()
        }
    }

    @Test
    fun `forwards query parameters`() {
        val backendPort = findFreePort()
        val backend = Javalin.create { it.routes.get("/search") { ctx -> ctx.result("q=${ctx.queryParam("q")}") } }.start(backendPort)
        val proxy = DevReloadProxy()
        proxy.targetPort = backendPort
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            val conn = URI("http://localhost:$proxyPort/search?q=test").toURL().openConnection() as HttpURLConnection
            assertThat(conn.inputStream.bufferedReader().readText()).isEqualTo("q=test")
        } finally {
            server.stop()
            backend.stop()
        }
    }

    @Test
    fun `forwards POST body`() {
        val backendPort = findFreePort()
        val backend = Javalin.create { it.routes.post("/data") { ctx -> ctx.result("got: ${ctx.body()}") } }.start(backendPort)
        val proxy = DevReloadProxy()
        proxy.targetPort = backendPort
        val proxyPort = findFreePort()
        val server = startProxyServer(proxy, proxyPort)
        try {
            val conn = URI("http://localhost:$proxyPort/data").toURL().openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.write("hello".toByteArray())
            conn.outputStream.close()
            assertThat(conn.responseCode).isEqualTo(200)
            assertThat(conn.inputStream.bufferedReader().readText()).isEqualTo("got: hello")
        } finally {
            server.stop()
            backend.stop()
        }
    }
}
