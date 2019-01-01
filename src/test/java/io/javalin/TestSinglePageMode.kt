/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.staticfiles.Location
import io.javalin.util.TestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File

class TestSinglePageMode {

    private val rootSinglePageApp_classPath: Javalin by lazy { Javalin.create().enableStaticFiles("/public").enableWebJars().enableSinglePageMode("/", "/public/html.html") }
    private val dualSinglePageApp_classPath: Javalin by lazy {
        Javalin.create().enableStaticFiles("/public")
                .enableSinglePageMode("/admin", "/public/protected/secret.html")
                .enableSinglePageMode("/public", "/public/html.html")
    }
    private val rootSinglePageApp_external: Javalin by lazy { Javalin.create().enableSinglePageMode("/", "src/test/external/html.html", Location.EXTERNAL) }

    @Test
    fun `SinglePageHandler works for HTML requests (classpath)`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").status, `is`(200))
    }

    @Test
    fun `SinglePageHandler doesn't affect static files (classpath)`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/script.js").headers.getFirst(Header.CONTENT_TYPE), containsString("application/javascript"))
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CONTENT_TYPE), containsString("text/css"))
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status, `is`(200))
    }

    @Test
    fun `SinglePageHandler doesn't affect JSON requests (classpath)`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.jsonGet("/").body, containsString("Not found"))
        assertThat(http.jsonGet("/not-a-file.html").body, containsString("Not found"))
        assertThat(http.jsonGet("/not-a-file.html").status, `is`(404))
    }

    @Test
    fun `SinglePageHandler works for just subpaths (classpath)`() = TestUtil.test(dualSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/").body, containsString("Not found"))
        assertThat(http.htmlGet("/").status, `is`(404))
        assertThat(http.htmlGet("/admin").body, containsString("Secret file"))
        assertThat(http.htmlGet("/admin/not-a-path").body, containsString("Secret file"))
        assertThat(http.htmlGet("/public").body, containsString("HTML works"))
        assertThat(http.htmlGet("/public/not-a-file.html").body, containsString("HTML works"))
        assertThat(http.htmlGet("/public/not-a-file.html").status, `is`(200))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SinglePageHandler throws for non-existent file (classpath)`() {
        Javalin.create().enableSinglePageMode("/", "/not-a-file.html").start().stop()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `SinglePageHandler throws for non-existent file (external)`() {
        Javalin.create().enableSinglePageMode("/", "/not-a-file.html", Location.EXTERNAL).start().stop()
    }

    @Test
    fun `SinglePageHandler works for HTML requests (external)`() = TestUtil.test(rootSinglePageApp_external) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").body, containsString("HTML works"))
        assertThat(http.htmlGet("/not-a-file.html").status, `is`(200))
    }

    @Test
    fun `SinglePageHandler doesn't cache on localhost`() {
        val filePath = "src/test/external/my-special-file.html"
        val file = File(filePath).apply { createNewFile() }.apply { writeText("OLD FILE") }
        val app = Javalin.create().enableSinglePageMode("/", filePath, Location.EXTERNAL).start()
        fun getSpaPage() = Unirest.get("http://localhost:${app.port()}/").header(Header.ACCEPT, "text/html").asString().body
        assertThat(getSpaPage(), containsString("OLD FILE"))
        file.writeText("NEW FILE")
        assertThat(getSpaPage(), containsString("NEW FILE"))
        app.stop()
        file.delete()
    }
}

