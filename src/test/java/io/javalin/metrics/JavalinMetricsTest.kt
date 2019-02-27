package io.javalin.metrics

import io.javalin.Javalin
import io.javalin.util.TestUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class JavalinMetricsTest {

    @Test
    fun `enable javalin metrics`() = TestUtil.test(Javalin.create().enableMetrics()) { app, http ->
        app.get("/test") { ctx -> ctx.result("Hello World with metrics enabled!") }
        assertEquals("Hello World with metrics enabled!", http.get("/test").body)
    }

    @Test
    fun `enable javalin metrics twice`() = TestUtil.test(Javalin.create().enableMetrics().enableMetrics()) { app, http ->
        app.get("/test") { ctx -> ctx.result("Hello World with metrics enabled!") }
        assertEquals("Hello World with metrics enabled!", http.get("/test").body)
    }
}
