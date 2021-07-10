/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.github.bonigarcia.wdm.WebDriverManager
import io.javalin.core.compression.Brotli
import io.javalin.core.util.Header
import io.javalin.http.util.SeekableWriter.chunkSize
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import io.javalin.testing.TestLoggingUtil.captureStdOut
import io.javalin.testing.TestUtil
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.AfterClass
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.io.File
import java.util.*
import kotlin.math.ceil

class TestWebBrowser {

    companion object {

        lateinit var driver: ChromeDriver

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            val os: String = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
            Assume.assumeTrue("mac" !in os && "darwin" !in os)
            WebDriverManager.chromedriver().setup()
            driver = ChromeDriver(ChromeOptions().apply {
                addArguments("--headless")
                addArguments("--disable-gpu")
            })
        }

        @AfterClass
        @JvmStatic
        fun teardownClass() {
            if (Companion::driver.isInitialized) {
                driver.quit()
            }
        }
    }

    @Test
    fun `hello world works in chrome`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello, Selenium!") }
        driver.get(http.origin + "/hello")
        assertThat(driver.pageSource).contains("Hello, Selenium")
    }

    @Test
    fun `brotli works in chrome`() {
        val payload = "Hello, Selenium!".repeat(150)
        val app = Javalin.create {
            it.compressionStrategy(Brotli(4), null)
            it.enableDevLogging()
        }.start(0)
        app.get("/hello") { ctx -> ctx.result(payload) }
        val logResult = captureStdOut { driver.get("http://localhost:" + app.port() + "/hello") }
        assertThat(driver.pageSource).contains(payload)
        assertThat(logResult).contains("Content-Encoding=br")
        assertThat(logResult).contains("Body is brotlied (${payload.length} bytes, not logged)")
        app.stop();
    }

    @Test
    fun `seeking works in chrome`() {
        chunkSize = 30000
        val file = File("src/test/resources/upload-test/sound.mp3")
        val expectedChunkCount = ceil(file.inputStream().available() / chunkSize.toDouble()).toInt()
        var chunkCount = 0
        val requestLoggerApp = Javalin.create {
            it.requestLogger { ctx, ms ->
                if (ctx.req.getHeader(Header.RANGE) == null) return@requestLogger
                chunkCount++
                // println("Req: " + ctx.req.getHeader(Header.RANGE))
                // println("Res: " + ctx.res.getHeader(Header.CONTENT_RANGE))
            }
        }
        TestUtil.test(requestLoggerApp) { app, http ->
            app.get("/file") { it.seekableStream(file.inputStream(), "audio/mpeg") }
            app.get("/audio-player") { it.html("""<audio src="/file"></audio>""") }
            driver.get(http.origin + "/audio-player")
            Thread.sleep(100) // so the logger has a chance to run
            assertThat(chunkCount).isEqualTo(expectedChunkCount)
            chunkSize = 128000
        }
    }

    @Test
    fun `path params are not html-encoded on the Vue prototype`() = TestUtil.test { app, http ->
        TestJavalinVue.before()
        app.get("/vue/:my-param", VueComponent("<test-component></test-component>"))
        driver.get(http.origin + "/vue/odd&co")

        val pathParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["my-param"]""") as String
        assertThat(pathParam).isEqualTo("odd&co")
        val nullParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.queryParams["my-param"]""") as String?
        assertThat(nullParam).isNull()
    }

    @Test
    fun `script tags in state function does not break page rendering`() = TestUtil.test { app, http ->
        TestJavalinVue.before()
        val testValue = "some value with <script></script> tags in it"
        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state", VueComponent("<test-component></test-component>"))
        driver.get(http.origin + "/script_in_state")
        val stateValue = driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        assertThat(stateValue).isEqualTo(testValue)
    }

    @Test
    fun `utf8 characters in state and parameters`() = TestUtil.test { app, http ->
        TestJavalinVue.before()
        val testValue = "some value with weird ✔️ \uD83C\uDF89 characters in it"
        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state/:param", VueComponent("<test-component></test-component>"))
        driver.get(http.origin + "/script_in_state/my_path_param_with_\uD83D\uDE80?☕=\uD83D\uDC95")
        val stateValue = driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        val pathParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["param"]""") as String
        val queryParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.queryParams["☕"]""") as List<String>

        assertThat(stateValue).isEqualTo(testValue)
        assertThat(pathParam).isEqualTo("my_path_param_with_\uD83D\uDE80")
        assertThat(queryParam).isEqualTo(listOf("\uD83D\uDC95"))
    }

    @Test
    fun `problematic characters in state and parameters`() = TestUtil.test { app, http ->
        TestJavalinVue.before()
        val testValue = "-_.!~*'()"

        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state/:param", VueComponent("<test-component></test-component>"))
        driver.get(http.origin + "/script_in_state/$testValue?my_key=$testValue")
        val stateValue = TestWebBrowser.driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        val pathParam = TestWebBrowser.driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["param"]""") as String
        val queryParam = TestWebBrowser.driver.executeScript("""return Vue.prototype.${"$"}javalin.queryParams["my_key"]""") as List<String>

        assertThat(stateValue).isEqualTo(testValue)
        assertThat(pathParam).isEqualTo(testValue)
        assertThat(queryParam).isEqualTo(listOf(testValue))
    }

}
