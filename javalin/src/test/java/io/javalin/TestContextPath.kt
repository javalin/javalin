/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import io.javalin.testing.get
import io.javalin.testing.httpCode
import io.javalin.util.Util
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
        val javalin = Javalin.create { it.router.contextPath = "/context-path" }
        TestUtil.test(javalin) { app, http ->
            app.get("/hello") { it.result("Hello World") }
            assertThat(http.get("/hello").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.getBody("/context-path/hello")).isEqualTo("Hello World")
        }
    }

    @Test
    fun `router works with multi-level context-path`() {
        val javalin = Javalin.create { it.router.contextPath = "/context-path/path-context" }
        TestUtil.test(javalin) { app, http ->
            app.get("/hello") { it.result("Hello World") }
            assertThat(http.get("/context-path/").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.getBody("/context-path/path-context/hello")).isEqualTo("Hello World")
        }
    }

    @Test
    fun `static-files work with context-path`() {
        val javalin = Javalin.create { servlet ->
            servlet.staticFiles.add("/public", Location.CLASSPATH)
            servlet.router.contextPath = "/context-path"
        }
        TestUtil.test(javalin) { _, http ->
            assertThat(http.get("/script.js").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.getBody("/context-path/script.js")).contains("JavaScript works")
        }
    }

    @Test
    fun `welcome-files work with context-path`() {
        val javalin = Javalin.create { servlet ->
            servlet.staticFiles.add("/public", Location.CLASSPATH)
            servlet.router.contextPath = "/context-path"
        }
        TestUtil.test(javalin) { _, http ->
            assertThat(http.getBody("/context-path/subdir/")).isEqualTo("<h1>Welcome file</h1>")
        }
    }

}
