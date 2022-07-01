/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.NotFoundResponse
import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.TestUtil
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * TODO: Enable when Micrometer will provide support for Jetty 11
 * ~ https://github.com/micrometer-metrics/micrometer/issues/3234
 */
@Disabled("Micrometer plugin is not supported on Jetty 11")
class TestMicrometerPlugin {

    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Test
    fun `test that JettyConnectionMetrics is registered`() {
        val registry = SimpleMeterRegistry()
        val micrometerApp = Javalin.create { it.registerPlugin(MicrometerPlugin(registry)) }

        TestUtil.test(micrometerApp) { app, http ->
            app.get("/test") { it.json("Hello world") }
            repeat(10) {
                http.get("/test")
            }
        }

        val maxConnectionGauge = registry.find("jetty.connections.max").gauge() ?: Assertions.fail("\"jetty.connections.max\" not found")
        assertThat(maxConnectionGauge.value()).isGreaterThan(0.0)

        val messagesOutCounter = registry.find("jetty.connections.messages.out").counter() ?: Assertions.fail("\"jetty.connections.messages.out\" not found")
        assertThat(messagesOutCounter.count()).isGreaterThan(0.0)
    }

    @Test
    fun `successful request`() = TestUtil.test(setupApp()) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello/{name}") { it.result("Hello: " + it.pathParam("name")) }
        repeat(requestCount) { http.get("/hello/jon") }
        val timerCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello/{name}")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(timerCount).isEqualTo(requestCount.toLong())
    }

    @Test
    fun `successful request with context path`() = TestUtil.test(setupApp(contextPath = "/api")) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.status(200) }
        repeat(requestCount) { http.get("/api/hello") }
        val timerCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(timerCount).isEqualTo(requestCount.toLong())
    }

    @Test
    fun `request throwing exception`() = TestUtil.test(setupApp()) { app, http ->
        val requestCount = (2..9).random()
        app.get("/boom") { throw IllegalArgumentException("boom") }
        repeat(requestCount) { http.get("/boom") }
        val timerCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/boom")
            .tag("method", "GET")
            .tag("exception", "IllegalArgumentException")
            .tag("status", "500")
            .tag("outcome", "SERVER_ERROR")
            .timer()
            .count()
        assertThat(timerCount).isEqualTo(requestCount.toLong())
    }

    @Test
    fun redirect() = TestUtil.test(setupApp()) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.result("Hello") }
        app.get("/redirect") { it.redirect("/hello") }
        repeat(requestCount) { http.get("/redirect") }
        val redirCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "REDIRECTION")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "302")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(redirCount + okCount).isEqualTo((requestCount * 2).toLong())
    }

    @Test
    fun `redirect tagged`() = TestUtil.test(setupApp(tagRedirectPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.result("Hello") }
        app.get("/redirect") { it.redirect("/hello") }
        repeat(requestCount) { http.get("/redirect") }
        val redirCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/redirect")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "302")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(redirCount + okCount).isEqualTo((requestCount * 2).toLong())
    }

    @Test
    fun `etags tagged`() = TestUtil.test(setupApp(tagRedirectPaths = true, autoGenerateEtags = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.result("Hello") }
        repeat(requestCount) {
            val response = http.get("/hello")
            val etag = response.headers["ETag"]?.first() ?: ""
            val response2 = http.get("/hello", mapOf("If-None-Match" to etag))
            assertThat(response2.status).isEqualTo(304)
        }
        val redirCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "304")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(redirCount + okCount).isEqualTo((requestCount * 2).toLong())
    }

    @Test
    fun `not found`() = TestUtil.test(setupApp()) { _, http ->
        val requestCount = (2..9).random()
        repeat(requestCount) { http.get("/some-unmapped-path") }
        val notFoundCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "NOT_FOUND")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()
        assertThat(notFoundCount).isEqualTo(requestCount.toLong())
    }

    @Test
    fun `not found tagged`() = TestUtil.test(setupApp(tagNotFoundMappedPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello/{name}") { ctx ->
            if (ctx.pathParam("name") == "jon") ctx.status(200)
            else throw NotFoundResponse()
        }
        repeat(requestCount) {
            http.get("/hello/jon")
            http.get("/hello/wil")
            http.get("/some-unmapped-path")
        }
        val okCount = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello/{name}")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        val notFoundCountSpecific = meterRegistry.get("jetty.server.requests")
            .tag("uri", "/hello/{name}")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()
        val notFoundCountGeneric = meterRegistry.get("jetty.server.requests")
            .tag("uri", "NOT_FOUND")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()
        assertThat(okCount).isEqualTo(requestCount.toLong())
        assertThat(notFoundCountSpecific).isEqualTo(requestCount.toLong())
        assertThat(notFoundCountGeneric).isEqualTo(requestCount.toLong())
    }

    private fun setupApp(
        tagRedirectPaths: Boolean = false,
        tagNotFoundMappedPaths: Boolean = false,
        autoGenerateEtags: Boolean? = null,
        contextPath: String = "/"
    ) = Javalin.create { config ->
        config.jetty.contextPath = contextPath
        config.registerPlugin(
            MicrometerPlugin(
                registry = meterRegistry,
                tags = Tags.empty(),
                tagExceptionName = true,
                tagRedirectPaths = tagRedirectPaths,
                tagNotFoundMappedPaths = tagNotFoundMappedPaths
            )
        )
        if (autoGenerateEtags != null) config.autogenerateEtags = autoGenerateEtags

        // must manually delegate to Micrometer exception handler for exception tags to be correct
    }.exception(IllegalArgumentException::class.java) { e, ctx ->
        MicrometerPlugin.EXCEPTION_HANDLER.handle(e, ctx)
    }

}
