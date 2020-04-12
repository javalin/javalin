/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Util
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.function.BiFunction
import java.util.function.Function

class TestContextPath {

    @Test
    fun `context-path is normalized`() {
        val normalize = Function<String, String> { Util.normalizeContextPath(it) }
        assertThat(normalize.apply("path")).isEqualTo("/path")
        assertThat(normalize.apply("/path")).isEqualTo("/path")
        assertThat(normalize.apply("/path/")).isEqualTo("/path")
        assertThat(normalize.apply("//path/")).isEqualTo("/path")
        assertThat(normalize.apply("/path//")).isEqualTo("/path")
        assertThat(normalize.apply("////path////")).isEqualTo("/path")
    }

    @Test
    fun `context-path is prefixed`() {
        val prefix = BiFunction<String, String, String> { contextPath, path -> Util.prefixContextPath(contextPath, path) }
        assertThat(prefix.apply("/c-p", "*")).isEqualTo("*")
        assertThat(prefix.apply("/c-p", "/*")).isEqualTo("/c-p/*")
        assertThat(prefix.apply("/c-p", "path")).isEqualTo("/c-p/path")
        assertThat(prefix.apply("/c-p", "/path")).isEqualTo("/c-p/path")
        assertThat(prefix.apply("/c-p", "//path")).isEqualTo("/c-p/path")
        assertThat(prefix.apply("/c-p", "/path/")).isEqualTo("/c-p/path/")
        assertThat(prefix.apply("/c-p", "//path//")).isEqualTo("/c-p/path/")
    }

    @Test
    fun `router works with context-path`() {
        val javalin = Javalin.create { it.contextPath = "/context-path" }
        TestUtil.test(javalin) { app, http ->
            app.get("/hello") { ctx -> ctx.result("Hello World") }
            assertThat(http.get("/hello").status).isEqualTo(404)
            assertThat(http.getBody("/context-path/hello")).isEqualTo("Hello World")
        }
    }

    @Test
    fun `router works with multi-level context-path`() {
        val javalin = Javalin.create { it.contextPath = "/context-path/path-context" }
        TestUtil.test(javalin) { app, http ->
            app.get("/hello") { ctx -> ctx.result("Hello World") }
            assertThat(http.get("/context-path/").status).isEqualTo(404)
            assertThat(http.getBody("/context-path/path-context/hello")).isEqualTo("Hello World")
        }
    }

    @Test
    fun `static-files work with context-path`() {
        val javalin = Javalin.create { servlet ->
            servlet.addStaticFiles("/public")
            servlet.contextPath = "/context-path"
        }
        TestUtil.test(javalin) { _, http ->
            assertThat(http.get("/script.js").status).isEqualTo(404)
            assertThat(http.getBody("/context-path/script.js")).contains("JavaScript works")
        }
    }

    @Test
    fun `welcome-files work with context-path`() {
        val javalin = Javalin.create { servlet ->
            servlet.addStaticFiles("/public")
            servlet.contextPath = "/context-path"
        }
        TestUtil.test(javalin) { _, http ->
            assertThat(http.getBody("/context-path/subdir/")).isEqualTo("<h1>Welcome file</h1>")
        }
    }

}
