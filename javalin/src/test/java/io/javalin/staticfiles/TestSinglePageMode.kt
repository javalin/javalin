/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.ContentType
import io.javalin.http.Context
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestDependency
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import io.javalin.testing.UnirestReplacement as Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestSinglePageMode {

    @TempDir
    lateinit var workingDirectory: File

    private val rootSinglePageApp_classPath: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.spaRoot.addFile("/", "/public/html.html")
            it.staticFiles.enableWebjars()
        }
    }

    private val dualSinglePageApp_classPath: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.spaRoot.addFile("/admin", "/public/protected/secret.html")
            it.spaRoot.addFile("/public", "/public/html.html")
        }

    }
    private val rootSinglePageApp_external: Javalin by lazy {
        Javalin.create {
            it.spaRoot.addFile("/", "src/test/external/html.html", Location.EXTERNAL)
        }
    }

    private val rootSinglePageCustomHandlerApp: Javalin by lazy {
        Javalin.create {
            it.spaRoot.addHandler("/") { ctx: Context ->
                ctx.result("Custom handler works")
                ctx.status(IM_A_TEAPOT)
            }
        }
    }

    private val mixedSinglePageHandlerApp: Javalin by lazy {
        Javalin.create {
            it.staticFiles.add("/public", Location.CLASSPATH)
            it.spaRoot.addFile("/public", "/public/html.html")
            it.spaRoot.addHandler("/public") { ctx: Context -> ctx.result("Will never be seen") }
            it.spaRoot.addHandler("/special") { ctx: Context ->
                ctx.result("Special custom handler works")
                ctx.status(IM_A_TEAPOT)
            }
        }
    }

    @Test
    fun `SinglePageHandler works for HTML requests - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `SinglePageHandler doesn't affect static files - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.htmlGet("/script.js").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.JAVASCRIPT)
        assertThat(http.htmlGet("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").headers.getFirst(
            Header.CONTENT_TYPE)).contains(ContentType.CSS)
        assertThat(http.htmlGet("/webjars/swagger-ui/${TestDependency.swaggerVersion}/swagger-ui.css").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `SinglePageHandler doesn't affect JSON requests - classpath`() = TestUtil.test(rootSinglePageApp_classPath) { _, http ->
        assertThat(http.jsonGet("/").body).contains("Endpoint GET / not found")
        assertThat(http.jsonGet("/not-a-file.html").body).contains("Endpoint GET /not-a-file.html not found")
        assertThat(http.jsonGet("/not-a-file.html").httpCode()).isEqualTo(NOT_FOUND)
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
        assertThat(http.htmlGet("/").body).contains("Endpoint GET / not found")
        assertThat(http.htmlGet("/").httpCode()).isEqualTo(NOT_FOUND)
        assertThat(http.htmlGet("/admin").body).contains("Secret file")
        assertThat(http.htmlGet("/admin/not-a-path").body).contains("Secret file")
        assertThat(http.htmlGet("/public").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-file.html").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `SinglePageHandler throws for non-existent file - classpath`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create { it.spaRoot.addFile("/", "/not-a-file.html") }.start().stop() }
            .withMessageStartingWith("File at '/not-a-file.html' not found. Path should be relative to resource folder.")
    }

    @Test
    fun `SinglePageHandler throws for non-existent file - external`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create { it.spaRoot.addFile("/", "/not-a-file.html", Location.EXTERNAL) }.start().stop() }
            .withMessageStartingWith("External file at '/not-a-file.html' not found.")
    }

    @Test
    fun `SinglePageHandler works for HTML requests - external`() = TestUtil.test(rootSinglePageApp_external) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("HTML works")
        assertThat(http.htmlGet("/not-a-file.html").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `SinglePageHandler doesn't cache on localhost`() {
        val file = File(workingDirectory, "my-special-file.html").also { it.writeText("old file") }
        TestUtil.test(Javalin.create { it.spaRoot.addFile("/", file.absolutePath, Location.EXTERNAL) }) { app, http ->
            fun getSpaPage() = http.get("/").let { response ->
                assertThat(response.status).isEqualTo(OK.code)
                response.body
            }
            assertThat(getSpaPage()).contains("old file")
            file.writeText("new file")
            assertThat(getSpaPage()).contains("new file")
        }
    }

    @Test
    fun `SinglePageHandler supports custom handler`() = TestUtil.test(rootSinglePageCustomHandlerApp) { _, http ->
        assertThat(http.htmlGet("/not-a-path").body).contains("Custom handler works")
        assertThat(http.htmlGet("/not-a-file.html").body).contains("Custom handler works")
        assertThat(http.htmlGet("/not-a-file.html").httpCode()).isEqualTo(IM_A_TEAPOT)
    }

    @Test
    fun `SinglePageHandler prefers filePath over customHandler`() = TestUtil.test(mixedSinglePageHandlerApp) { _, http ->
        assertThat(http.htmlGet("/public").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-path").body).contains("HTML works")
        assertThat(http.htmlGet("/public/not-a-path").httpCode()).isEqualTo(OK)
        assertThat(http.htmlGet("/special").body).contains("Special custom handler works")
        assertThat(http.htmlGet("/special/not-a-file.html").body).contains("Special custom handler works")
        assertThat(http.htmlGet("/special/not-a-file.html").httpCode()).isEqualTo(IM_A_TEAPOT)
    }
}
