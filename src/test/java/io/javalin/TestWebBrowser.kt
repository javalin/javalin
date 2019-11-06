/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.github.bonigarcia.wdm.WebDriverManager
import io.javalin.core.compression.Brotli
import io.javalin.misc.captureStdOut
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.AfterClass
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.util.*

class TestWebBrowser {

    companion object {

        lateinit var driver: WebDriver

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
    fun `hello world works in chrome`() {
        val app = Javalin.create().start(0)
        app.get("/hello") { ctx -> ctx.result("Hello, Selenium!") }
        driver.get("http://localhost:" + app.port() + "/hello")
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
    }

}
