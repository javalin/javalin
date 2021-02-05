/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.NotFoundResponse
import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.HttpUtil
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestMicrometerPlugin {
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()

    @Test
    fun `successful request`() {
        val app: Javalin = setupApp()
        val http = HttpUtil(app.port())

        app.get("/hello/:name") { ctx -> ctx.result("Hello: " + ctx.pathParam("name")) }
        http.get("/hello/jon")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello/:name")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()

        app.stop()
    }

    @Test
    fun `request throwing exception`() {
        val app: Javalin = setupApp()
        val http = HttpUtil(app.port())

        app.get("/boom") { throw IllegalArgumentException("boom") }
        http.get("/boom")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/boom")
                .tag("method", "GET")
                .tag("exception", "IllegalArgumentException")
                .tag("status", "500")
                .tag("outcome", "SERVER_ERROR")
                .timer()

        app.stop()
    }

    @Test
    fun redirect() {
        val app: Javalin = setupApp()
        val http = HttpUtil(app.port())

        app.get("/hello") { ctx -> ctx.result("Hello") }
        app.get("/redirect") { ctx -> ctx.redirect("/hello") }
        http.get("/redirect")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "REDIRECTION")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "302")
                .tag("outcome", "REDIRECTION")
                .timer()
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()

        app.stop()
    }

    @Test
    fun `redirect tagged`() {
        val app: Javalin = setupApp(tagRedirectPaths = true)
        val http = HttpUtil(app.port())

        app.get("/hello") { ctx -> ctx.result("Hello") }
        app.get("/redirect") { ctx -> ctx.redirect("/hello") }
        http.get("/redirect")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/redirect")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "302")
                .tag("outcome", "REDIRECTION")
                .timer()
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()

        app.stop()
    }

    @Test
    fun `etags tagged`() {
        val app: Javalin = setupApp(tagRedirectPaths = true, autoGenerateEtags = true)
        val http = HttpUtil(app.port())

        app.get("/hello") { ctx -> ctx.result("Hello") }

        val response = http.get("/hello")
        val etag = response.headers["ETag"]?.first() ?: ""

        val response2 = http.get("/hello", mapOf("If-None-Match" to etag))
        assertThat(response2.status).isEqualTo(304)

        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "304")
                .tag("outcome", "REDIRECTION")
                .timer()
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()

        app.stop()
    }

    @Test
    fun `not found`() {
        val app: Javalin = setupApp()
        val http = HttpUtil(app.port())

        http.get("/doesNotExist")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "NOT_FOUND")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "404")
                .tag("outcome", "CLIENT_ERROR")
                .timer()

        app.stop()
    }

    @Test
    fun `not found tagged`() {
        val app: Javalin = setupApp(tagNotFoundMappedPaths = true)
        val http = HttpUtil(app.port())

        app.get("/hello/:name") { ctx ->
            if (ctx.pathParam("name") == "jon") ctx.result("Hello: " + ctx.pathParam("name"))
            else throw NotFoundResponse()
        }

        http.get("/hello/jon")
        http.get("/hello/wil")
        http.get("/doesNotExist")

        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello/:name")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()

        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello/:name")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "404")
                .tag("outcome", "CLIENT_ERROR")
                .timer()

        meterRegistry.get("jetty.server.requests")
                .tag("uri", "NOT_FOUND")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "404")
                .tag("outcome", "CLIENT_ERROR")
                .timer()

        app.stop()
    }

    private fun setupApp(tagRedirectPaths: Boolean = false,
                         tagNotFoundMappedPaths: Boolean = false,
                         autoGenerateEtags: Boolean? = null) =
            Javalin.create { config ->
                config.registerPlugin(MicrometerPlugin(registry = meterRegistry,
                        tags = Tags.empty(),
                        tagExceptionName = true,
                        tagRedirectPaths = tagRedirectPaths,
                        tagNotFoundMappedPaths = tagNotFoundMappedPaths)
                )
                if (autoGenerateEtags != null) config.autogenerateEtags = autoGenerateEtags

                // must manually delegate to Micrometer exception handler for exception tags to be correct
            }.start(0).exception(IllegalArgumentException::class.java) { e, ctx ->
                MicrometerPlugin.EXCEPTION_HANDLER.handle(e, ctx)
                e.printStackTrace()
            }
}
