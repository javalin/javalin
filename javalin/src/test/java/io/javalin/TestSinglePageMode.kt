/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import java.io.File

class TestSinglePageMode {

    private val rootSinglePageApp_classPath: Javalin by lazy {
        Javalin.create {
            it.addStaticFiles("/public")
            it.addSinglePageRoot("/", "/public/html.html")
            it.enableWebjars()
        }
    }

    private val dualSinglePageApp_classPath: Javalin by lazy {
        Javalin.create {
            it.addStaticFiles("/public")
            it.addSinglePageRoot("/admin", "/public/protected/secret.html")
            it.addSinglePageRoot("/public", "/public/html.html")
        }

    }
    private val rootSinglePageApp_external: Javalin by lazy {
        Javalin.create {
            it.addSinglePageRoot("/", "src/test/external/html.html", Location.EXTERNAL)
        }
    }

    private val rootSinglePageCustomHandlerApp: Javalin by lazy {
        Javalin.create {
            it.addSinglePageHandler("/") { ctx: Context ->
                ctx.result("Custom handler works")
                ctx.status(418)
            }
        }
    }

    private val mixedSinglePageHandlerApp: Javalin by lazy {
        Javalin.create {
            it.addStaticFiles("/public")
            it.addSinglePageRoot("/public", "/public/html.html")
            it.addSinglePageHandler("/public") { ctx: Context -> ctx.result("Will never be seen") }
            it.addSinglePageHandler("/special") { ctx: Context ->
                ctx.result("Special custom handler works")
                ctx.status(418)
            }
        }
    }

    @Test
    fun `SinglePageHandler works for HTML requests - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").status).isEqualTo(200)
    }

    @Test
    fun `SinglePageHandler doesn't affect static files - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains("application/javascript")
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").headers.getFirst(Header.CONTENT_TYPE)).contains("text/css")
        assertThat(http.htmlGet("/webjars/swagger-ui/${OptionalDependency.SWAGGERUI.version}/swagger-ui.css").status).isEqualTo(200)
    }

    @Test
    fun `SinglePageHandler doesn't affect JSON requests - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.jsonGet("/").body).contains("Not found")
        assertThat(http.jsonGet("/not-a-file.html").body).contains("Not found")
        assertThat(http.jsonGet("/not-a-file.html").status).isEqualTo(404)
    }

    @Test
    fun `SinglePageHandler works when accepts is blank or star-slash-star`() = TestUtil.test(rootSinglePageApp_classPath) { app, http ->
        val blankAcceptsResponseBody = Unirest.get("http://localhost:${app.port()}/not-a-file").header(Header.ACCEPT, "").asString().body
        assertThat(blankAcceptsResponseBody).contains("HTML works")
        val starSlashStarAcceptsResponseBody = Unirest.get("http://localhost:${app.port()}/not-a-file").header(Header.ACCEPT, "*/*").asString().body
        assertThat(starSlashStarAcceptsResponseBody).contains("HTML works")
    }

    @Test
    fun `SinglePageHandler works when accepts contains star-slash-star`() = TestUtil.test(rootSinglePageApp_classPath) { app, http ->
        val ieAcceptHeader = "image/jpeg, application/x-ms-application, image/gif, application/xaml+xml, image/pjpeg, application/x-ms-xbap, application/x-shockwave-flash, application/msword, */*"
        val starSlashStarAcceptsResponseBody = Unirest.get("http://localhost:${app.port()}/not-a-file").header(Header.ACCEPT, ieAcceptHeader).asString().body
        assertThat(starSlashStarAcceptsResponseBody).contains("HTML works")
    }

    @Test
    fun `SinglePageHandler works for just subpaths - classpath`() = TestUtil.test(dualSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/").body).contains("Not found")
        assertThat(http.htmlGet("/").status).isEqualTo(404)
        assertThat(http.htmlGet("/admin").body).contains("Secret file")
        assertThat(http.htmlGet("/admin/not-a-path").body).contains("Secret file")
        assertThat(http.htmlGet("/public").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-file.html").status).isEqualTo(200)
    }

    @Test
    fun `SinglePageHandler throws for non-existent file - classpath`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { Javalin.create { it.addSinglePageRoot("/", "/not-a-file.html") }.start().stop() }
                .withMessageStartingWith("File at '/not-a-file.html' not found. Path should be relative to resource folder.")
    }

    @Test
    fun `SinglePageHandler throws for non-existent file - external`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { Javalin.create { it.addSinglePageRoot("/", "/not-a-file.html", Location.EXTERNAL) }.start().stop() }
                .withMessageStartingWith("External file at '/not-a-file.html' not found.")
    }

    @Test
    fun `SinglePageHandler works for HTML requests - external`() = TestUtil.test(rootSinglePageApp_external) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").status).isEqualTo(200)
    }

    @Test
    fun `SinglePageHandler doesn't cache on localhost`() {
        val filePath = "src/test/external/my-special-file.html"
        val file = File(filePath).apply { createNewFile() }.apply { writeText("OLD FILE") }
        val app = Javalin.create { it.addSinglePageRoot("/", filePath, Location.EXTERNAL) }.start(0)
        fun getSpaPage() = Unirest.get("http://localhost:${app.port()}/").header(Header.ACCEPT, "text/html").asString().body
        assertThat(getSpaPage()).contains("OLD FILE")
        file.writeText("NEW FILE")
        assertThat(getSpaPage()).contains("NEW FILE")
        app.stop()
        file.delete()
    }

    @Test
    fun `SinglePageHandler supports custom handler`() = TestUtil.test(rootSinglePageCustomHandlerApp) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("Custom handler works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("Custom handler works")
        assertThat(http.htmlGet("/not-a-file.html").status).isEqualTo(418)
    }

    @Test
    fun `SinglePageHandler prefers filePath over customHandler`() = TestUtil.test(mixedSinglePageHandlerApp) { _, http ->
        assertThat(http.htmlGet("/public").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-path").status).isEqualTo(200)
        assertThat(http.htmlGet("/special").body).contains("Special custom handler works")
        assertThat(http.htmlGet("/special/not-a-file.html").body).contains("Special custom handler works")
        assertThat(http.htmlGet("/special/not-a-file.html").status).isEqualTo(418)
    }
}
