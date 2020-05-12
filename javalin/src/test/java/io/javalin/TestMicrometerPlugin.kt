/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.metrics.MicrometerPlugin
import io.javalin.testing.HttpUtil
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test

class TestMicrometerPlugin {
    private val meterRegistry: MeterRegistry = SimpleMeterRegistry()
    val app: Javalin = Javalin.create { config -> config.registerPlugin(MicrometerPlugin(meterRegistry, Tags.empty(), true)) }.start(0);
    val http = HttpUtil(app.port())

    @Before
    fun before() {
        // must manually delegate to Micrometer exception handler for excepton tags to be correct
        app.exception(IllegalArgumentException::class.java) { e, ctx ->
            MicrometerPlugin.EXCEPTION_HANDLER.handle(e, ctx);
            e.printStackTrace();
        };
    }

    @After
    fun after() {
        app.stop()
    }

    @Test
    fun `successful request`() {
        app.get("/hello/:name") { ctx -> ctx.result("Hello: " + ctx.pathParam("name")) }
        http.get("/hello/jon")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/hello/:name")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "200")
                .tag("outcome", "SUCCESS")
                .timer()
    }

    @Test
    fun `request throwing exception`() {
        app.get("/boom") { ctx -> throw IllegalArgumentException("boom") }
        http.get("/boom")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "/boom")
                .tag("method", "GET")
                .tag("exception", "IllegalArgumentException")
                .tag("status", "500")
                .tag("outcome", "SERVER_ERROR")
                .timer()
    }

    @Test
    fun redirect() {
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
    }

    @Test
    fun `not found`() {
        http.get("/doesNotExist")
        meterRegistry.get("jetty.server.requests")
                .tag("uri", "NOT_FOUND")
                .tag("method", "GET")
                .tag("exception", "None")
                .tag("status", "404")
                .tag("outcome", "CLIENT_ERROR")
                .timer()
    }
}
