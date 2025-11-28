/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip
import io.javalin.http.Header
import io.javalin.http.util.SeekableWriter.chunkSize
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.captureStdOut
import io.javalin.testing.WebDriverUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.chrome.ChromeDriver
import java.io.File
import kotlin.math.ceil

class TestWebBrowser {

    private lateinit var driver: ChromeDriver

    @BeforeEach
    fun setup() {
        driver = WebDriverUtil.getDriver()
    }

    @AfterEach
    fun teardown() {
        if (::driver.isInitialized) { // we don't always initialize the driver when running locally
            driver.quit()
        }
    }

    @Test
    fun `hello world works in chrome`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/hello") { it.result("Hello, Selenium!") }
        driver.get(http.origin + "/hello")
        assertThat(driver.pageSource).contains("Hello, Selenium")
    }

    @Test
    fun `brotli works in chrome`() {
        TestUtil.runAndCaptureLogs {
            val payload = "Hello, Selenium!".repeat(150)
            val app = Javalin.create {
                it.http.compressionStrategy = CompressionStrategy(Brotli())
                it.registerPlugin(DevLoggingPlugin())
            }.start(0)
            app.unsafe.routes.get("/hello") { it.result(payload) }
            val logResult = captureStdOut {
                driver.get("http://localhost:" + app.port() + "/hello")
            }
            assertThat(driver.pageSource).contains(payload)
            assertThat(logResult).contains("Content-Encoding=br")
            assertThat(logResult).contains("Body is brotlied (${payload.length} bytes, not logged)")
            app.stop()
        }
    }

    @Test
    fun `seeking works in chrome`() {
        chunkSize = 30000
        val file = File("src/test/resources/upload-test/sound.mp3")
        val expectedChunkCount = ceil(file.inputStream().available() / chunkSize.toDouble()).toInt()
        var chunkCount = 0
        val requestLoggerApp = Javalin.create {
            it.requestLogger.http { ctx, ms ->
                if (ctx.req().getHeader(Header.RANGE) == null) return@http
                chunkCount++
                // println("Req: " + ctx.req.getHeader(Header.RANGE))
                // println("Res: " + ctx.res().getHeader(Header.CONTENT_RANGE))
            }
        }
        TestUtil.test(requestLoggerApp) { app, http ->
            app.unsafe.routes.get("/file") { it.writeSeekableStream(file.inputStream(), "audio/mpeg") }
            app.unsafe.routes.get("/audio-player") { it.html("""<audio src="/file"></audio>""") }
            driver.get(http.origin + "/audio-player")
            Thread.sleep(100) // so the logger has a chance to run
            assertThat(chunkCount).isEqualTo(expectedChunkCount)
            chunkSize = 128000
        }
    }

    @Test
    fun `chrome can handle precompressed files GH-1958`() = TestUtil.test(Javalin.create { config ->
        config.http.compressionStrategy = CompressionStrategy(Brotli(), Gzip())
        config.staticFiles.add { staticFiles ->
            staticFiles.hostedPath = "/"
            staticFiles.directory = "/public"
            staticFiles.precompress = true
        }
    }) { _, http ->
        driver.get(http.origin + "/html.html")
        val html = driver.findElement(By.tagName("html")).getAttribute("innerHTML")
        Assertions.assertThat(html).contains("HTML works")
        Assertions.assertThat(html).contains("JavaScript works")
    }

}
