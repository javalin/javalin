/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.MOVED_PERMANENTLY
import io.javalin.http.HttpStatus.SEE_OTHER
import io.javalin.http.util.SeekableWriter
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.HttpMethod
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIOException
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TestResponse {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `setting a String result works`() = TestUtil.test { app, http ->
        val myBody = """
            This is my body, and I live in it. It's 31 and 6 months old.
            It's changed a lot since it was new. It's done stuff it wasn't built to do.
            I often try to fill if up with wine. - Tim Minchin
        """
        app.get("/hello") { ctx ->
            ctx.status(IM_A_TEAPOT).result(myBody).header("X-HEADER-1", "my-header-1").header("X-HEADER-2", "my-header-2")
        }
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.httpCode()).isEqualTo(IM_A_TEAPOT)
        assertThat(response.body).isEqualTo(myBody)
        assertThat(response.headers.getFirst("X-HEADER-1")).isEqualTo("my-header-1")
        assertThat(response.headers.getFirst("X-HEADER-2")).isEqualTo("my-header-2")
    }

    @Test
    fun `setting a byte array result works`() = TestUtil.test { app, http ->
        val bytes = ByteArray(512)

        for (i in 0 until 512) {
            bytes[i] = (i % 256).toByte()
        }

        app.get("/hello") { it.result(bytes) }
        val response = Unirest.get("${http.origin}/hello").asBytes()
        assertThat(response.body.size).isEqualTo(bytes.size)
        assertThat(bytes).isEqualTo(response.body)
    }

    @Test
    fun `setting an InputStream result works`() = TestUtil.test { app, http ->
        val buf = ByteArray(65537) // big and not on a page boundary
        Random().nextBytes(buf)
        app.get("/stream") { it.result(ByteArrayInputStream(buf)) }
        val response = Unirest.get("${http.origin}/stream").asBytes()
        assertThat(response.body.size).isEqualTo(buf.size)
        assertThat(buf).isEqualTo(response.body)
    }

    @Test
    fun `setting an InputStream result works and InputStream is closed`() = TestUtil.test { app, http ->
        val file = File(workingDirectory, "my-file.txt").also {
            it.printWriter().use { out ->
                out.print("Hello, World!")
            }
        }
        val inputStream = file.inputStream()

        app.get("/file") { ctx ->
            ctx.result(inputStream)
        }

        assertThat(http.getBody("/file")).isEqualTo("Hello, World!")
        // Expecting an IOException for reading a closed stream
        assertThatIOException()
            .isThrownBy { inputStream.read() }
    }

    @Disabled("https://github.com/tipsy/javalin/pull/1413")
    @Test
    fun `gh-1409 entrypoint to analyze compression strategy lifecycle`() {
        val javalin = Javalin.create { javalinConfig ->
            javalinConfig.plugins.enableCors { cors -> cors.add { it.reflectClientOrigin = true } }
            javalinConfig.showJavalinBanner = false
            javalinConfig.http.maxRequestSize = 5_000_000
        }.start(9005)

        val longString = Array(Short.MAX_VALUE.toInt()) { "0" }.joinToString()

        javalin.get("/route") { ctx ->
            ctx.result(longString)
        }

        while (true) {
            // should be requested by external tool
        }
    }

    @Test
    fun `redirect in before-handler works`() = TestUtil.test { app, http ->
        app.before("/before") { it.redirect("/redirected") }
        app.get("/redirected") { it.result("Redirected") }
        assertThat(http.getBody("/before")).isEqualTo("Redirected")
    }

    @Test
    fun `redirect in exception-mapper works`() = TestUtil.test { app, http ->
        app.get("/get") { throw Exception() }
        app.exception(Exception::class.java) { _, ctx -> ctx.redirect("/redirected") }
        app.get("/redirected") { it.result("Redirected") }
        assertThat(http.getBody("/get")).isEqualTo("Redirected")
    }

    @Test
    fun `redirect in normal handler works`() = TestUtil.test { app, http ->
        http.disableUnirestRedirects()
        app.get("/hello") { it.redirect("/hello-2") }
        app.get("/hello-2") { it.result("Woop!") }
        val response = http.get("/hello")
        assertThat(response.body).isEqualTo("Redirected")
        assertThat(response.status).isEqualTo(HttpStatus.FOUND.code)
        http.enableUnirestRedirects()
    }

    @Test
    fun `redirect with status works`() = TestUtil.test { app, http ->
        http.disableUnirestRedirects()
        app.get("/hello") { it.redirect("/hello-2", MOVED_PERMANENTLY) }
        app.get("/hello-2") { it.result("Redirected") }
        assertThat(http.call(HttpMethod.GET, "/hello").httpCode()).isEqualTo(MOVED_PERMANENTLY)
        assertThat(http.call(HttpMethod.GET, "/hello").body).isEqualTo("Redirected")
        http.enableUnirestRedirects()
    }

    @Test
    fun `redirect to absolute path works`() = TestUtil.test { app, http ->
        app.get("/hello-abs") { it.redirect("${http.origin}/hello-abs-2", SEE_OTHER) }
        app.get("/hello-abs-2") { it.result("Redirected") }
        http.disableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello-abs").httpCode()).isEqualTo(SEE_OTHER)
        http.enableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello-abs").body).isEqualTo("Redirected")
    }

    // Fix for https://github.com/tipsy/javalin/issues/543
    @Test
    fun `reading the result string resets the stream`() = TestUtil.test { app, http ->
        val result = "Hello World"

        app.get("/test") { context ->
            context.result(result)
            context.result()
        }

        assertThat(http.getBody("/test")).isEqualTo(result)
    }

    private fun getSeekableInput(repeats: Int = SeekableWriter.chunkSize) = object : ByteArrayInputStream(
        setOf("a", "b", "c").joinToString("") { it.repeat(repeats) }.toByteArray(Charsets.UTF_8)
    ) {
        val closedLatch = CountDownLatch(1)

        override fun close() {
            super.close()
            closedLatch.countDown()
        }
    }

    @Test
    fun `seekable - range works and input stream closed`() = TestUtil.test { app, http ->
        val input = getSeekableInput()
        app.get("/seekable") { it.writeSeekableStream(input, ContentType.PLAIN) }
        val response = Unirest.get(http.origin + "/seekable")
            .headers(mapOf(Header.RANGE to "bytes=${SeekableWriter.chunkSize}-${SeekableWriter.chunkSize * 2 - 1}"))
            .asString().body
        assertThat(response).doesNotContain("a").contains("b").doesNotContain("c")
        assertThat(input.closedLatch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `seekable - no-range works and input stream closed`() = TestUtil.test { app, http ->
        val input = getSeekableInput()
        val available = input.available()
        app.get("/seekable-2") { it.writeSeekableStream(input, ContentType.PLAIN) }
        val response = Unirest.get(http.origin + "/seekable-2").asString().body
        assertThat(response.length).isEqualTo(available)
        assertThat(input.closedLatch.await(2, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun `seekable - overreaching range works`() = TestUtil.test { app, http ->
        app.get("/seekable-3") { it.writeSeekableStream(getSeekableInput(), ContentType.PLAIN) }
        val response = Unirest.get(http.origin + "/seekable-3")
            .headers(mapOf(Header.RANGE to "bytes=0-${SeekableWriter.chunkSize * 4}"))
            .asBytes()
        assertThat(response.body.size).isEqualTo(SeekableWriter.chunkSize * 3)
    }

    @Test
    fun `seekable - file smaller than chunksize works`() = TestUtil.test { app, http ->
        app.get("/seekable-4") { it.writeSeekableStream(getSeekableInput(repeats = 50), ContentType.PLAIN) }
        val response = Unirest.get(http.origin + "/seekable-4")
            .headers(mapOf(Header.RANGE to "bytes=0-${SeekableWriter.chunkSize}"))
            .asString().body
        assertThat(response.length).isEqualTo(150)
    }

    @Test
    fun `seekable - large file works`() = TestUtil.test { app, http ->
        val prefixSize = 1L shl 31 //2GB
        val contentSize = 100L
        app.get("/seekable-5") { it.writeSeekableStream(LargeSeekableInput(prefixSize, contentSize), ContentType.PLAIN, prefixSize + contentSize) }
        val response = Unirest.get(http.origin + "/seekable-5")
            .headers(mapOf(Header.RANGE to "bytes=${prefixSize}-${prefixSize + contentSize - 1}"))
            .asString()

        assertThat(response.headers[Header.CONTENT_RANGE]?.get(0)).isEqualTo("bytes ${prefixSize}-${prefixSize + contentSize - 1}/${prefixSize + contentSize}")
        val responseBody = response.body
        assertThat(responseBody.length).isEqualTo(contentSize)
        assertThat(responseBody).doesNotContain(" ")
    }

}
