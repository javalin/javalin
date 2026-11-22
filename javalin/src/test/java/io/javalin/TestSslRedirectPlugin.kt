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
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

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
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin).asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:8443/")
    }

    @Test
    fun `ssl redirect plugin only changes the scheme`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/httpclient") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin + "/httpclient?next=http://example.com").asEmpty()
        assertThat(response.status).isEqualTo(301)
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://localhost:${app.port()}/httpclient?next=http://example.com")
    }

    @Test
    fun `ssl redirect plugin only changes the scheme of an https url`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
            cfg.contextResolver.url = { "https://example.com/" }
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val response = httpWithoutAutoRedirects.get(http.origin + "/?next=http://example.com").header(Header.X_FORWARDED_PROTO, "http").asEmpty()
        assertThat(response.headers.getFirst(Header.LOCATION)).isEqualTo("https://example.com/?next=http://example.com")
    }

    @Test
    fun `ssl redirect plugin reads X-Forwarded-Proto case-insensitively and from a list`() = TestUtil.test(
        Javalin.create { cfg ->
            cfg.registerPlugin(SslRedirectPlugin {
                it.redirectOnLocalhost = true
            })
        }
    ) { app, http ->
        app.unsafe.routes.get("/") { ctx -> ctx.result("Hello") }

        val statuses = listOf("HTTP", "http, https").map { httpWithoutAutoRedirects.get(http.origin).header(Header.X_FORWARDED_PROTO, it).asEmpty().status }
        assertThat(statuses).containsExactly(301, 301)
        assertThat(httpWithoutAutoRedirects.get(http.origin).header(Header.X_FORWARDED_PROTO, "https").asEmpty().status).isEqualTo(200)
    }

}
