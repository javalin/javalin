package io.javalin

import io.javalin.http.Header
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
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
        app.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin).asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:${app.port()}/")
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
        app.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin).asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:8443/")
    }

}
