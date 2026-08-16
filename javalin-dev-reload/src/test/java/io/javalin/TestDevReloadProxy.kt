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

    private fun get(port: Int, path: String): HttpURLConnection =
        URI("http://localhost:$port$path").toURL().openConnection() as HttpURLConnection

    private fun HttpURLConnection.body(): String =
        (if (responseCode < 400) inputStream else errorStream).bufferedReader().readText()

    /** Runs a test with a proxy server, stopping everything on completion. */
    private fun withProxy(
        configure: DevReloadProxy.() -> Unit = {},
        backends: List<Javalin> = emptyList(),
        test: (proxy: DevReloadProxy, port: Int) -> Unit,
    ) {
        val proxy = DevReloadProxy().apply(configure)
        val proxyPort = findFreePort()
        val server = Server(proxyPort)
        val handler = ServletContextHandler()
        handler.addFilter(FilterHolder(proxy), "/*", EnumSet.of(DispatcherType.REQUEST))
        server.handler = handler
        server.start()
        try {
            test(proxy, proxyPort)
        } finally {
            server.stop()
            backends.forEach { it.stop() }
        }
    }

    private fun startBackend(path: String, response: String): Javalin {
        val port = findFreePort()
        return Javalin.create { it.routes.get(path) { ctx -> ctx.result(response) } }.start(port)
    }

    @Test
    fun `returns 503 when no target port set`() = withProxy { _, port ->
        val conn = get(port, "/hello")
        assertThat(conn.responseCode).isEqualTo(503)
        assertThat(conn.body()).contains("Recompiling")
    }

    @Test
    fun `forwards request to target when port is set`() {
        val backend = startBackend("/hello", "backend-response")
        withProxy(configure = { targetPort = backend.port() }, backends = listOf(backend)) { _, port ->
            val conn = get(port, "/hello")
            assertThat(conn.responseCode).isEqualTo(200)
            assertThat(conn.body()).isEqualTo("backend-response")
        }
    }

    @Test
    fun `returns 503 when target port becomes unavailable`() = withProxy(configure = {
        targetPort = findFreePort() // nothing listening
    }) { _, port ->
        assertThat(get(port, "/hello").responseCode).isEqualTo(503)
    }

    @Test
    fun `proxy can switch targets`() {
        val b1 = startBackend("/hello", "v1")
        val b2 = startBackend("/hello", "v2")
        withProxy(configure = { targetPort = b1.port() }, backends = listOf(b1, b2)) { proxy, port ->
            assertThat(get(port, "/hello").body()).isEqualTo("v1")
            proxy.targetPort = b2.port()
            assertThat(get(port, "/hello").body()).isEqualTo("v2")
        }
    }

    @Test
    fun `forwards query parameters`() {
        val backend = Javalin.create { it.routes.get("/search") { ctx -> ctx.result("q=${ctx.queryParam("q")}") } }
            .start(findFreePort())
        withProxy(configure = { targetPort = backend.port() }, backends = listOf(backend)) { _, port ->
            assertThat(get(port, "/search?q=test").body()).isEqualTo("q=test")
        }
    }

    @Test
    fun `status endpoint returns error JSON on compile failure`() = withProxy(configure = {
        targetPort = findFreePort()
        reloadingFiles = listOf("Foo.kt")
        compileError = "Foo.kt:10: error: expecting ')'"
    }) { _, port ->
        val conn = get(port, "/__dev-reload/status")
        assertThat(conn.responseCode).isEqualTo(503)
        val body = conn.body()
        assertThat(body).contains("\"ready\":false")
        assertThat(body).contains("\"error\":")
        assertThat(body).contains("expecting ')'")
    }

    @Test
    fun `shows compile error page then recovers on fix`() {
        val backend = startBackend("/", "ok")
        withProxy(configure = { targetPort = backend.port() }, backends = listOf(backend)) { proxy, port ->
            // Simulate compile error
            proxy.reloadingFiles = listOf("Bar.kt")
            proxy.compileError = "Bar.kt:1: error: unresolved reference"
            assertThat(get(port, "/").responseCode).isEqualTo(503)
            assertThat(get(port, "/").body()).contains("Compile Error")

            // Simulate successful recompile
            proxy.compileError = null
            proxy.reloadingFiles = emptyList()
            assertThat(get(port, "/").body()).isEqualTo("ok")
            assertThat(get(port, "/__dev-reload/status").body()).contains("\"ready\":true")
        }
    }

    @Test
    fun `forwards POST body`() {
        val backend = Javalin.create { it.routes.post("/data") { ctx -> ctx.result("got: ${ctx.body()}") } }
            .start(findFreePort())
        withProxy(configure = { targetPort = backend.port() }, backends = listOf(backend)) { _, port ->
            val conn = get(port, "/data")
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.outputStream.write("hello".toByteArray())
            conn.outputStream.close()
            assertThat(conn.responseCode).isEqualTo(200)
            assertThat(conn.body()).isEqualTo("got: hello")
        }
    }
}
