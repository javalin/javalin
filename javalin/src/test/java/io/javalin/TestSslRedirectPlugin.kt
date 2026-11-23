package io.javalin

import io.javalin.http.Header
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ForwardedRequestCustomizer
import org.junit.jupiter.api.Test

class TestSslRedirectPlugin {

    private val httpWithoutAutoRedirects = Unirest.spawnInstance()

    init {
        httpWithoutAutoRedirects.config().followRedirects(false)
    }

    @Test
    fun `ssl redirect plugin should redirect all http requests`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin).asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:${app.port()}/")
    }

    @Test
    fun `ssl redirect plugin redirects a forwarded non-loopback client that sends Host localhost`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin())
            cfg.jetty.modifyHttpConfiguration { it.addCustomizer(ForwardedRequestCustomizer()) }
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }
        assertThat(httpWithoutAutoRedirects.get(http.origin).asEmpty().status).isEqualTo(200)
        assertThat(httpWithoutAutoRedirects.get(http.origin).header("X-Forwarded-For", "203.0.113.7").asEmpty().status).isEqualTo(301)
    }

    @Test
    fun `ssl redirect plugin should support custom ssl port`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
                it.sslPort = 8443
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin).asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:8443/")
    }

}
