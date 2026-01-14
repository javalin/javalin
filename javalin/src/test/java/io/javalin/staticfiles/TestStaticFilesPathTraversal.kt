/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/javalin/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

/**
 * Tests for directory traversal attacks on static files.
 * Jetty blocks most simple traversal attempts (../, %2e%2e/) at the HTTP level,
 * so these tests focus on double-encoded paths that bypass Jetty's checks.
 * We also test the simple attempts to ensure they remain blocked if Jetty changes.
 */
class TestStaticFilesPathTraversal {

    @TempDir
    lateinit var tempDir: File

    private val classpathStaticFilesConfig = { cfg: JavalinConfig ->
        cfg.staticFiles.add("/public", Location.CLASSPATH)
    }

    private val jdkClient = HttpClient.newHttpClient()

    /** Raw HTTP request using JDK client - doesn't normalize paths like Unirest */
    private fun rawGet(origin: String, path: String): Pair<Int, String> {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$origin$path"))
                .GET()
                .build()
            val response = jdkClient.send(request, BodyHandlers.ofString())
            response.statusCode() to response.body()
        } catch (e: Exception) {
            // Connection reset or similar = blocked at HTTP level
            -1 to "blocked: ${e.message}"
        }
    }

    /** Asserts request is blocked - either by Jetty (400/connection reset) or app (404) */
    private fun assertBlocked(origin: String, path: String) {
        val (status, _) = rawGet(origin, path)
        assertThat(status).isIn(-1, 400, 404) // -1 = connection reset, 400 = bad request, 404 = not found
    }

    // Simple traversal - currently blocked by Jetty, but test in case that changes
    @Test
    fun `blocks basic dot-dot-slash traversal`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertBlocked(http.origin, "/../secret.txt")
        assertBlocked(http.origin, "/subdir/../../../secret.txt")
    }

    @Test
    fun `blocks URL-encoded dot-dot-slash traversal`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertBlocked(http.origin, "/%2e%2e/secret.txt")
        assertBlocked(http.origin, "/%2e%2e%2fsecret.txt")
    }

    // Double-encoded - bypasses Jetty, must be blocked by app
    @Test
    fun `blocks double URL-encoded traversal`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertThat(http.get("/%252e%252e/secret.txt").status).isEqualTo(NOT_FOUND.code)
        assertThat(http.get("/%252e%252e%252fsecret.txt").status).isEqualTo(NOT_FOUND.code)
    }

    @Test
    fun `blocks URL-encoded backslash traversal`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertThat(http.get("/..%5csecret.txt").status).isEqualTo(NOT_FOUND.code)
        assertThat(http.get("/%2e%2e%5csecret.txt").status).isEqualTo(NOT_FOUND.code)
    }

    @Test
    fun `blocks null byte injection`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertBlocked(http.origin, "/html.html%00.jpg")
        assertBlocked(http.origin, "/../secret.txt%00.html")
    }

    @Test
    fun `allows valid file access`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertThat(http.get("/html.html").status).isEqualTo(OK.code)
        assertThat(http.get("/html.html").body).contains("HTML works")
    }

    @Test
    fun `allows valid subdirectory access`() = testStaticFiles(classpathStaticFilesConfig) { _, http ->
        assertThat(http.get("/subdir/").status).isEqualTo(OK.code)
        assertThat(http.get("/protected/secret.html").status).isEqualTo(OK.code)
    }

    @Test
    fun `blocks traversal for external static files`() {
        val publicDir = File(tempDir.toPath().toRealPath().toFile(), "public").apply { mkdirs() }
        File(tempDir.toPath().toRealPath().toFile(), "secret.txt").apply { writeText("SECRET DATA") }
        File(publicDir, "index.html").apply { writeText("Public content") }

        val externalConfig = { cfg: JavalinConfig ->
            cfg.staticFiles.add(publicDir.absolutePath, Location.EXTERNAL)
        }

        testStaticFiles(externalConfig) { _, http ->
            assertThat(http.get("/index.html").status).isEqualTo(OK.code)
            assertThat(http.get("/index.html").body).isEqualTo("Public content")
            assertBlocked(http.origin, "/../secret.txt")
            assertThat(http.get("/%252e%252e/secret.txt").status).isEqualTo(NOT_FOUND.code)
        }
    }
}

