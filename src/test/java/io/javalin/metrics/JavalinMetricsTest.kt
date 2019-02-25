package io.javalin.metrics

import io.javalin.Javalin
import io.javalin.util.TestUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class JavalinMetricsTest {

    @Test
    fun `enable javalin metrics`() = TestUtil.test(Javalin.create().enableMetrics()) { app, http ->
        app.get("/test") { ctx -> ctx.result("Hello World!") }
        assertEquals("Hello World!", http.get("/test").body)
    }
}
