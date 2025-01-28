package io.javalin.micrometer
/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

import io.javalin.Javalin
import io.javalin.http.HttpStatus.OK
import io.javalin.http.NotFoundResponse
import io.javalin.testtools.JavalinTest
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MicrometerPluginTest {

    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Test
    fun `test that JettyConnectionMetrics is registered`() {
        val registry = SimpleMeterRegistry()
        val micrometerApp = Javalin.create { cfg -> cfg.registerPlugin(MicrometerPlugin { it.registry = registry }) }

        JavalinTest.test(micrometerApp) { app, http ->
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
    fun `successful request`() = JavalinTest.test(setupApp()) { app, http ->
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
    fun `successful request with context path`() = JavalinTest.test(setupApp(contextPath = "/api")) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.status(OK) }
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
    fun `request throwing exception`() = JavalinTest.test(setupApp()) { app, http ->
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
    fun redirect() = JavalinTest.test(setupApp()) { app, http ->
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
    fun `redirect tagged`() = JavalinTest.test(setupApp(tagRedirectPaths = true)) { app, http ->
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
    fun `etags tagged`() = JavalinTest.test(setupApp(tagRedirectPaths = true, autoGenerateEtags = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { it.result("Hello") }
        repeat(requestCount) {
            val response = http.get("/hello")
            val etag = when {
                response.headers["ETag"]?.first() != null -> response.headers["ETag"]
                else -> ""
            }
            val response2 = http.get("/hello") {
                it.header("If-None-Match", etag!!)
            }
            assertThat(response2.code).isEqualTo(304) // NOT MODIFIED
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
    fun `not found`() = JavalinTest.test(setupApp()) { _, http ->
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
    fun `not found tagged`() = JavalinTest.test(setupApp(tagNotFoundMappedPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello/{name}") { ctx ->
            if (ctx.pathParam("name") == "jon") ctx.status(OK)
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

    @Test
    fun invalidMethod() = JavalinTest.test(setupApp(tagNotFoundMappedPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.get("/hello") { ctx ->
            ctx.status(OK)
        }
        repeat(requestCount) {
            http.request("/hello") { b -> b.method("POSTS", null) }
        }

        val notFoundCountGeneric = meterRegistry.get("jetty.server.requests")
            .tag("uri", "NOT_FOUND")
            .tag("method", "POSTS")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()

        assertThat(notFoundCountGeneric).isEqualTo(requestCount.toLong())
    }

    private fun setupApp(
        tagRedirectPaths: Boolean = false,
        tagNotFoundMappedPaths: Boolean = false,
        autoGenerateEtags: Boolean? = null,
        contextPath: String = "/"
    ) = Javalin.create { config ->
        config.router.contextPath = contextPath
        config.registerPlugin(MicrometerPlugin {
            it.registry = meterRegistry
            it.tags = Tags.empty()
            it.tagExceptionName = true
            it.tagRedirectPaths = tagRedirectPaths
            it.tagNotFoundMappedPaths = tagNotFoundMappedPaths
        })
        if (autoGenerateEtags != null) config.http.generateEtags = autoGenerateEtags

        // must manually delegate to Micrometer exception handler for exception tags to be correct
    }.exception(IllegalArgumentException::class.java) { e, ctx ->
        MicrometerPlugin.exceptionHandler.handle(e, ctx)
    }

}
