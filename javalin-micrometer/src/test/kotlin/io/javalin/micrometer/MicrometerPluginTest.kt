package io.javalin.micrometer

import io.javalin.Javalin
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.NotFoundResponse
import io.javalin.testtools.JavalinTest
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import io.javalin.testtools.TestConfig
import java.net.http.HttpClient
import java.time.Duration


class MicrometerPluginTest {

    private val meterRegistry = SimpleMeterRegistry()

    private val followRedirectsConfig = TestConfig(
        httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    )

    @Test
    fun `plugin can be registered`() {
        val app = Javalin.create { config ->
            config.registerPlugin(MicrometerPlugin {
                it.registry = meterRegistry
            })
            config.routes.get("/test") { ctx -> ctx.result("Hello World") }
        }
        app.stop()
        assertThat(app).isNotNull()
    }

    @Test
    fun `basic request metrics are recorded`() = JavalinTest.test(setupApp()) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello") { it.status(OK) }
        repeat(requestCount) { http.get("/hello") }

        val timerCount = meterRegistry.get("http.server.requests")
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
        app.unsafe.routes.get("/boom") { throw IllegalArgumentException("boom") }
        repeat(requestCount) { http.get("/boom") }

        val timerCount = meterRegistry.get("http.server.requests")
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
    fun `redirect metrics`() = JavalinTest.test(setupApp(), followRedirectsConfig) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello") { it.result("Hello") }
        app.unsafe.routes.get("/redirect") { it.redirect("/hello") }
        repeat(requestCount) { http.get("/redirect") }

        val redirCount = meterRegistry.get("http.server.requests")
            .tag("uri", "REDIRECTION")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "302")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("http.server.requests")
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
    fun `redirect tagged when enabled`() = JavalinTest.test(setupApp(tagRedirectPaths = true), followRedirectsConfig) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello") { it.result("Hello") }
        app.unsafe.routes.get("/redirect") { it.redirect("/hello") }
        repeat(requestCount) { http.get("/redirect") }

        val redirCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/redirect")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "302")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("http.server.requests")
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
        app.unsafe.routes.get("/hello") { it.result("Hello") }
        repeat(requestCount) {
            val response = http.get("/hello")
            val etag = response.headers().get("ETag")?.firstOrNull().orEmpty()
            val response2 = http.get("/hello") {
                it.header("If-None-Match", etag)
            }
            assertThat(response2.code).isEqualTo(304) // NOT MODIFIED
        }

        val notModifiedCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "304")
            .tag("outcome", "REDIRECTION")
            .timer()
            .count()
        val okCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(notModifiedCount + okCount).isEqualTo((requestCount * 2).toLong())
    }

    @Test
    fun `not found`() = JavalinTest.test(setupApp()) { _, http ->
        val requestCount = (2..9).random()
        repeat(requestCount) { http.get("/some-unmapped-path") }

        val notFoundCount = meterRegistry.get("http.server.requests")
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
    fun `not found tagged when enabled`() = JavalinTest.test(setupApp(tagNotFoundMappedPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello/{name}") { ctx ->
            if (ctx.pathParam("name") == "jon") ctx.status(OK)
            else throw NotFoundResponse()
        }
        repeat(requestCount) {
            http.get("/hello/jon")
            http.get("/hello/wil")
            http.get("/some-unmapped-path")
        }

        val okCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello/{name}")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        val notFoundCountSpecific = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello/{name}")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()
        val notFoundCountGeneric = meterRegistry.get("http.server.requests")
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
    fun `invalid method`() = JavalinTest.test(setupApp(tagNotFoundMappedPaths = true)) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello") { ctx ->
            ctx.status(OK)
        }
        repeat(requestCount) {
            http.request("/hello") { b -> b.header("X-Http-Method-Override", "POSTS") }
        }

        val notFoundCountGeneric = meterRegistry.get("http.server.requests")
            .tag("uri", "NOT_FOUND")
            .tag("method", "POSTS")
            .tag("exception", "None")
            .tag("status", "404")
            .tag("outcome", "CLIENT_ERROR")
            .timer()
            .count()

        assertThat(notFoundCountGeneric).isEqualTo(requestCount.toLong())
    }

    @Test
    fun `custom tags are applied`() = JavalinTest.test(setupApp(customTags = Tags.of("service", "test-app", "env", "test"))) { app, http ->
        app.unsafe.routes.get("/hello") { it.status(OK) }
        http.get("/hello")

        val timerCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("service", "test-app")
            .tag("env", "test")
            .timer()
            .count()
        assertThat(timerCount).isEqualTo(1L)
    }

    @Test
    fun `context path is handled correctly`() = JavalinTest.test(setupApp(contextPath = "/api")) { app, http ->
        val requestCount = (2..9).random()
        app.unsafe.routes.get("/hello") { it.status(OK) }
        repeat(requestCount) { http.get("/api/hello") }

        val timerCount = meterRegistry.get("http.server.requests")
            .tag("uri", "/hello")
            .tag("method", "GET")
            .tag("exception", "None")
            .tag("status", "200")
            .tag("outcome", "SUCCESS")
            .timer()
            .count()
        assertThat(timerCount).isEqualTo(requestCount.toLong())
    }

    private fun setupApp(
        tagRedirectPaths: Boolean = false,
        tagNotFoundMappedPaths: Boolean = false,
        autoGenerateEtags: Boolean? = null,
        contextPath: String = "/",
        customTags: Tags = Tags.empty()
    ) = Javalin.create { config ->
        config.router.contextPath = contextPath
        config.registerPlugin(MicrometerPlugin {
            it.registry = meterRegistry
            it.tags = customTags
            it.tagExceptionName = true
            it.tagRedirectPaths = tagRedirectPaths
            it.tagNotFoundMappedPaths = tagNotFoundMappedPaths
        })
        config.routes.exception(IllegalArgumentException::class.java) { e, ctx ->
            // Must manually delegate to Micrometer exception handler for exception tags to be correct
            MicrometerPlugin.exceptionHandler.handle(e, ctx)
        }
        if (autoGenerateEtags != null) config.http.generateEtags = autoGenerateEtags
    }
}
