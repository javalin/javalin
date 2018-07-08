/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Util
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Test
import java.util.function.BiFunction
import java.util.function.Function

class TestContextPath {

    @Test
    fun `context-path is normalized`() {
        val normalize = Function<String, String> { Util.normalizeContextPath(it) }
        assertThat(normalize.apply("path"), `is`("/path"))
        assertThat(normalize.apply("/path"), `is`("/path"))
        assertThat(normalize.apply("/path/"), `is`("/path"))
        assertThat(normalize.apply("//path/"), `is`("/path"))
        assertThat(normalize.apply("/path//"), `is`("/path"))
        assertThat(normalize.apply("////path////"), `is`("/path"))
    }

    @Test
    fun `context-path is prefixed`() {
        val prefix = BiFunction<String, String, String> { contextPath, path -> Util.prefixContextPath(contextPath, path) }
        assertThat(prefix.apply("/c-p", "*"), `is`("*"))
        assertThat(prefix.apply("/c-p", "/*"), `is`("/c-p/*"))
        assertThat(prefix.apply("/c-p", "path"), `is`("/c-p/path"))
        assertThat(prefix.apply("/c-p", "/path"), `is`("/c-p/path"))
        assertThat(prefix.apply("/c-p", "//path"), `is`("/c-p/path"))
        assertThat(prefix.apply("/c-p", "/path/"), `is`("/c-p/path/"))
        assertThat(prefix.apply("/c-p", "//path//"), `is`("/c-p/path/"))
    }

    @Test
    fun `router works with context -path`() = TestUtil.test(Javalin.create().contextPath("/context-path")) { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello World") }
        assertThat(http.getBody("/hello"), `is`("Not found. Request is below context-path (context-path: '/context-path')"))
        assertThat(http.getBody("/context-path/hello"), `is`("Hello World"))
    }

    @Test
    fun `router works with multi-level context-path`() = TestUtil.test(Javalin.create().contextPath("/context-path/path-context")) { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello World") }
        assertThat(http.get("/context-path/").status, `is`(404))
        assertThat(http.getBody("/context-path/path-context/hello"), `is`("Hello World"))
    }

    @Test
    fun `static-files work with context-path`() = TestUtil.test(Javalin.create().contextPath("/context-path").enableStaticFiles("/public")) { app, http ->
        assertThat(http.get("/script.js").status, `is`(404))
        assertThat(http.getBody("/context-path/script.js"), containsString("JavaScript works"))
    }

    @Test
    fun `welcome-files work with context-path`() = TestUtil.test(Javalin.create().contextPath("/context-path").enableStaticFiles("/public")) { app, http ->
        assertThat(http.getBody("/context-path/subdir/"), `is`("<h1>Welcome file</h1>"))
    }

}
