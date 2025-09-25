package io.javalin

import io.javalin.http.Header
import io.javalin.plugin.bundled.SslRedirectPlugin
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestSslRedirectPlugin {

    @Test
    fun `ssl redirect plugin should redirect all http requests`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
        }
    ) { app, http ->
        app.get("/") { ctx -> ctx.result("Hello") }

        http.disableUnirestRedirects()
        val response = http.get("/")
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:${app.port()}/")
        http.enableUnirestRedirects()
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

        http.disableUnirestRedirects()
        val response = http.get("/")
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:8443/")
        http.enableUnirestRedirects()
    }

}
