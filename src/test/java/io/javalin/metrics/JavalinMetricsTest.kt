package io.javalin.metrics

import io.javalin.Javalin
import io.javalin.util.TestUtil
import org.junit.After
import org.junit.Before

import org.junit.Assert.*
import org.junit.Test

class JavalinMetricsTest {

    @Before
    @Throws(Exception::class)
    fun setUp() {
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun `enable javalin metrics`() = TestUtil.test(Javalin.create().enableMetrics()) { app, http ->
        app.get("/test") { ctx -> ctx.result("Hello World!") }
        assertEquals("Hello World!", http.get("/test").body)
    }
}
