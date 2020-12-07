/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.github.bonigarcia.wdm.WebDriverManager
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import io.javalin.http.util.SeekableWriter.chunkSize
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import io.javalin.testing.TestLoggingUtil.captureStdOut
import io.javalin.testing.TestUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.nio.file.Paths
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.AfterClass
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.WebDriver

class TestJavalinVueWebBrowser {

    @Before
    fun setup() {
        JavalinVue.isDev = null // reset
        JavalinVue.stateFunction = { ctx -> mapOf<String, String>() } // reset
        JavalinVue.rootDirectory("src/test/resources/vue", Location.EXTERNAL) // src/main -> src/test
        JavalinVue.optimizeDependencies = false
    }

    data class User(val name: String, val email: String)
    data class Role(val name: String)
    data class State(val user: User, val role: Role)

    private val state = State(User("tipsy", "tipsy@tipsy.tipsy"), Role("Maintainer"))


    companion object {

        lateinit var driver: ChromeDriver

        @BeforeClass
        @JvmStatic
        fun setupClass() {
            val os: String = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH)
            Assume.assumeTrue("mac" !in os && "darwin" !in os)
            WebDriverManager.chromedriver().setup()
            driver = ChromeDriver(ChromeOptions().apply {
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
    fun `path params are not html-encoded on the client`() = TestUtil.test { app, http ->
        app.get("/vue/:zeisty", VueComponent("<path-params-in-body-component></path-params-in-body-component>"))
        driver.get(http.origin + "/vue/odd&co")
        assertThat(driver.pageSource).contains(""""zeisty":"odd&amp;co"},""") // source loaded on client, before js is executed
        assertThat(driver.pageSource).contains("""<div id="test">odd&co</div>""")
        val pathParams = driver.executeScript("return Vue.prototype.\$javalin.pathParams[\"my-param\"]") as String
        assertThat(pathParams).contains("odd&co")
    }
}
