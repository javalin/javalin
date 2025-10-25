package io.javalin

import io.javalin.http.Header
import io.javalin.plugin.SslRedirectPlugin
import io.javalin.testing.header
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestSslRedirectPlugin {

    @Test
    fun `ssl redirect plugin should redirect all http requests`() = JavalinTest.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = http.get("/")
        assertThat(response.code()).isEqualTo(301)
        // The redirect location will use 127.0.0.1 since that's what the HttpClient uses
        val location = response.header(Header.LOCATION)
        assertThat(location).matches("https://(localhost|127\\.0\\.0\\.1):${app.port()}/")
    }

    @Test
    fun `ssl redirect plugin should support custom ssl port`() = JavalinTest.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
                it.sslPort = 8443
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = http.get("/")
        assertThat(response.code()).isEqualTo(301)
        // The redirect location will use 127.0.0.1 since that's what the HttpClient uses
        val location = response.header(Header.LOCATION)
        assertThat(location).matches("https://(localhost|127\\.0\\.0\\.1):8443/")
    }

}
