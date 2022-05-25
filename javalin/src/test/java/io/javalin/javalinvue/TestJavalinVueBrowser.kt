package io.javalin.javalinvue

import io.github.bonigarcia.wdm.WebDriverManager
import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.util.*

class TestJavalinVueBrowser {

    @BeforeEach
    fun beforeEach() = TestJavalinVue.before()

    companion object {
        lateinit var driver: ChromeDriver

        @BeforeAll
        @JvmStatic
        fun setupClass() {
            assumeTrue(System.getProperty("RunningOnCi") == null)
            val os: String = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
            assumeTrue("mac" !in os && "darwin" !in os)
            WebDriverManager.chromedriver().setup()
            driver = ChromeDriver(ChromeOptions().apply {
                addArguments("--no-sandbox")
                addArguments("--headless")
                addArguments("--disable-gpu")
            })
        }

        @AfterAll
        @JvmStatic
        fun teardownClass() {
            if (Companion::driver.isInitialized) {
                driver.quit()
            }
        }

    }

    @Test
    fun `path params are not html-encoded on the Vue prototype`() = TestUtil.test { app, http ->
        app.get("/vue/{my-param}", VueComponent("test-component"))
        driver.get(http.origin + "/vue/odd&co")
        val pathParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["my-param"]""") as String
        assertThat(pathParam).isEqualTo("odd&co")
    }

    @Test
    fun `script tags in state function does not break page rendering`() = TestUtil.test { app, http ->
        val testValue = "some value with <script></script> tags in it"
        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state", VueComponent("test-component"))
        driver.get(http.origin + "/script_in_state")
        val stateValue = driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        assertThat(stateValue).isEqualTo(testValue)
    }

    @Test
    fun `utf8 characters in state and parameters`() = TestUtil.test { app, http ->
        val testValue = "some value with weird ✔️ \uD83C\uDF89 characters in it"
        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state/{param}", VueComponent("test-component"))
        driver.get(http.origin + "/script_in_state/my_path_param_with_\uD83D\uDE80")
        val stateValue = driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        val pathParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["param"]""") as String
        assertThat(stateValue).isEqualTo(testValue)
        assertThat(pathParam).isEqualTo("my_path_param_with_\uD83D\uDE80")
    }

    @Test
    fun `problematic characters in state and parameters`() = TestUtil.test { app, http ->
        val testValue = "-_.!~*'()"
        JavalinVue.stateFunction = {
            mapOf("some_key" to testValue)
        }
        app.get("/script_in_state/{param}", VueComponent("test-component"))
        driver.get(http.origin + "/script_in_state/$testValue")
        val stateValue = driver.executeScript("""return Vue.prototype.${"$"}javalin.state["some_key"]""") as String
        val pathParam = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["param"]""") as String
        assertThat(stateValue).isEqualTo(testValue)
        assertThat(pathParam).isEqualTo(testValue)
    }

    /* LoadableData tests below here */

    fun loadableDataTestApp() = Javalin.create().routes {
        val users = mutableListOf("John")
        get("/api/users") { it.json(users) }
        get("/api/otherUsers") { it.json(users) }
        get("/api/add-mary") { users.add("Mary") } // easier than posting from selenium
        get("/ld", VueComponent("test-component"))
    }

    @Test
    fun `LoadableData loads data correctly`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.executeScript("localStorage.clear()")
        driver.createLoadableData("ld", "new LoadableData('/api/users')")
        assertThat(driver.checkWindow("ld.loading === false")).isTrue()
        assertThat(driver.checkWindow("ld.loadError === null")).isTrue()
        assertThat(driver.checkWindow("ld.data.length === 1")).isTrue()
        assertThat(driver.checkWindow("localStorage.length === 1")).isTrue() // cache is true by default
        driver.createMaryOnBackend()
        driver.executeScript("window.ld.refresh()")
        driver.waitForCondition("ld.loaded")
        assertThat(driver.checkWindow("ld.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData errors on bad url`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/wrong-url')")
        assertThat(driver.checkWindow("ld.loadError !== null")).isTrue()
    }

    @Test
    fun `LoadableData cache can be disabled`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.executeScript("localStorage.clear()")
        driver.createLoadableData("ld", "new LoadableData('/api/users', false)")
        assertThat(driver.checkWindow("localStorage.length === 0")).isTrue()
    }

    @Test
    fun `LoadableData errorCallback works`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/wrong-url', false, () => window.myVar = 'Error')")
        assertThat(driver.checkWindow("ld.loadError.code === 404")).isTrue()
        assertThat(driver.checkWindow("myVar === 'Error'")).isTrue()
    }

    @Test
    fun `LoadableData instance can refresh itself`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/api/users')")
        driver.createMaryOnBackend()
        driver.executeScript("window.ld.refresh()")
        driver.waitForCondition("ld.loaded") // refresh sets this to false
        assertThat(driver.checkWindow("ld.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData instance can be refreshed through static method`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/api/users')")
        driver.createMaryOnBackend()
        driver.executeScript("LoadableData.refreshAll('/api/users')")
        driver.waitForCondition("ld.loaded") // refresh sets this to false
        assertThat(driver.checkWindow("ld.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData instance can refresh instances with same URL`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld1", "new LoadableData('/api/users')")
        driver.createLoadableData("ld2", "new LoadableData('/api/users')")
        driver.createMaryOnBackend()
        driver.executeScript("window.ld1.refreshAll()")
        driver.waitForCondition("ld1.loaded") // refresh sets this to false
        driver.waitForCondition("ld2.loaded") // refresh sets this to false
        assertThat(driver.checkWindow("ld1.data.length === 2")).isTrue()
        assertThat(driver.checkWindow("ld2.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData instance can not refresh instances with different URL`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld1", "new LoadableData('/api/users')")
        driver.createLoadableData("ld2", "new LoadableData('/api/otherUsers')")
        driver.createMaryOnBackend()
        driver.executeScript("window.ld1.refreshAll()")
        driver.waitForCondition("ld1.loaded") // refresh sets this to false
        driver.waitForCondition("ld2.loaded") // refresh sets this to false, although here it will remain false
        assertThat(driver.checkWindow("ld1.data.length === 2")).isTrue()
        assertThat(driver.checkWindow("ld2.data.length === 1")).isTrue()
    }

    private fun ChromeDriver.createLoadableData(name: String, variable: String) {
        this.executeScript("""window.${name} = $variable""")
        while (checkWindow("${name}.loaded === false") && checkWindow("${name}.loadError === null")) Thread.sleep(10)
    }

    private fun ChromeDriver.createMaryOnBackend() = this.executeScript("await fetch('/api/add-mary')")

    private fun ChromeDriver.waitForCondition(jsCode: String) {
        while (!checkWindow(jsCode)) Thread.sleep(10)
    }

    private fun ChromeDriver.checkWindow(jsCode: String) = this.executeScript("return window.$jsCode") as Boolean

}
