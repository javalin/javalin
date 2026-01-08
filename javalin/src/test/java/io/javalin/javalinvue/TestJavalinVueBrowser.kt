package io.javalin.javalinvue

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.staticfiles.Location
import io.javalin.plugin.bundled.JavalinVuePlugin
import io.javalin.testing.TestUtil
import io.javalin.testing.WebDriverUtil
import io.javalin.vue.VueComponent
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.chrome.ChromeDriver

class TestJavalinVueBrowser {

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
    fun `loadabledata and state works when csp is enabled`() = VueTestUtil.test({
        it.enableCspAndNonces = true
        it.enableLoadableData = true
    }, null) { app, http ->
        app.unsafe.routes.get("/vue/{my-param}", VueComponent("test-component"))
        driver.get(http.origin + "/vue/odd&co")
        driver.executeScript("let ld = new LoadableData()") // would throw if loadable data was removed by CSP
        val pathParam = driver.executeScript("""return JavalinVue.pathParams["my-param"]""") as String
        assertThat(pathParam).isEqualTo("odd&co")
    }

    @Test
    fun `script tag without nonce is loaded if csp is not enabled`() = VueTestUtil.test(null, null) { app, http ->
        app.unsafe.routes.get("/vue", VueComponent("test-component"))
        driver.get(http.origin + "/vue")
        val stringFromLayoutHtml = driver.executeScript("return noncelessString") as String
        assertThat(stringFromLayoutHtml).isEqualTo("abc")
    }

    @Test
    fun `script tag without nonce is not loaded if csp is enabled`() = VueTestUtil.test({
        it.enableCspAndNonces = true
    }, null) { app, http ->
        app.unsafe.routes.get("/vue", VueComponent("test-component"))
        driver.get(http.origin + "/vue")
        Assertions.assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { driver.executeScript("return noncelessString") }
            .withMessageContaining("javascript error: noncelessString is not defined")
    }

    @Test
    fun `path params are not html-encoded on JavalinVue`() = VueTestUtil.test(null, null) { app, http ->
        app.unsafe.routes.get("/vue/{my-param}", VueComponent("test-component"))
        driver.get(http.origin + "/vue/odd&co")
        val pathParam = driver.executeScript("""return JavalinVue.pathParams["my-param"]""") as String
        assertThat(pathParam).isEqualTo("odd&co")
    }

    @Test
    fun `the Vue prototype has the same values as JavalinVue`() = VueTestUtil.test(null, null) { app, http ->
        app.unsafe.routes.get("/vue/{my-param}", VueComponent("test-component"))
        driver.get(http.origin + "/vue/odd&co")
        val p1 = driver.executeScript("""return JavalinVue.pathParams["my-param"]""") as String
        val p2 = driver.executeScript("""return Vue.prototype.${"$"}javalin.pathParams["my-param"]""") as String
        assertThat(p1).isEqualTo("odd&co")
        assertThat(p1).isEqualTo(p2)
    }

    @Test
    fun `script tags in state function does not break page rendering`() {
        val testValue = "some value with <script></script> tags in it"
        VueTestUtil.test({ it.stateFunction = { mapOf("some_key" to testValue) } }, null) { app, http ->
            app.unsafe.routes.get("/script_in_state", VueComponent("test-component"))
            driver.get(http.origin + "/script_in_state")
            val stateValue = driver.executeScript("""return JavalinVue.state["some_key"]""") as String
            assertThat(stateValue).isEqualTo(testValue)
        }
    }

    @Test
    fun `utf8 characters in state and parameters`() {
        val testValue = "some value with weird ✔️ \uD83C\uDF89 characters in it"
        VueTestUtil.test({ it.stateFunction = { mapOf("some_key" to testValue) } }, null) { app, http ->
            app.unsafe.routes.get("/script_in_state/{param}", VueComponent("test-component"))
            driver.get(http.origin + "/script_in_state/my_path_param_with_\uD83D\uDE80")
            val stateValue = driver.executeScript("""return JavalinVue.state["some_key"]""") as String
            val pathParam = driver.executeScript("""return JavalinVue.pathParams["param"]""") as String
            assertThat(stateValue).isEqualTo(testValue)
            assertThat(pathParam).isEqualTo("my_path_param_with_\uD83D\uDE80")
        }
    }


    @Test
    fun `problematic characters in state and parameters`() {
        val testValue = "-_.!~*'()"
        VueTestUtil.test({ it.stateFunction = { mapOf("some_key" to testValue) } }, null) { app, http ->
            app.unsafe.routes.get("/script_in_state/{param}", VueComponent("test-component"))
            driver.get(http.origin + "/script_in_state/$testValue")
            val stateValue = driver.executeScript("""return JavalinVue.state["some_key"]""") as String
            val pathParam = driver.executeScript("""return JavalinVue.pathParams["param"]""") as String
            assertThat(stateValue).isEqualTo(testValue)
            assertThat(pathParam).isEqualTo(testValue)
        }
    }

    /* LoadableData tests below here */
    private fun loadableDataTestApp() = Javalin.create {
        it.registerPlugin(JavalinVuePlugin { vue ->
            vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
            vue.enableLoadableData = true
        })
        it.routes.apiBuilder {
            val users = mutableListOf("John")
            get("/api/users") { it.json(users) }
            get("/api/otherUsers") { it.json(users) }
            get("/api/add-mary") { users.add("Mary") } // easier than posting from selenium
            get("/ld", VueComponent("test-component"))
        }
    }

    @Test
    fun `LoadableData loads data correctly`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.executeScript("localStorage.clear()")
        driver.createLoadableData("ld", "new LoadableData('/api/users')")
        assertThat(driver.checkWindow("ld.loading === false")).isTrue()
        assertThat(driver.checkWindow("ld.loadError === null")).isTrue()
        assertThat(driver.checkWindow("ld.data.length === 1")).isTrue()
        assertThat(driver.checkWindow("localStorage.length === 0")).isTrue() // cache is false by default
        driver.createMaryOnBackend()
        driver.executeScript("window.ld.refresh()")
        driver.waitForCondition("ld.refreshing === false")
        assertThat(driver.checkWindow("ld.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData errors on bad url`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/wrong-url')")
        assertThat(driver.checkWindow("ld.loadError !== null")).isTrue()
    }

    @Test
    fun `LoadableData cache can be enabled`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.executeScript("localStorage.clear()")
        driver.createLoadableData("ld", "new LoadableData('/api/users', {cache: true})")
        assertThat(driver.checkWindow("localStorage.length === 1")).isTrue()
    }

    @Test
    fun `LoadableData cache can be invalidated`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.executeScript("localStorage.clear()")
        driver.createLoadableData("ld", "new LoadableData('/api/users', {cache: true})")
        assertThat(driver.checkWindow("localStorage.length === 1")).isTrue()
        driver.executeScript("window.ld.invalidateCache()")
        assertThat(driver.checkWindow("localStorage.length === 0")).isTrue()
    }

    @Test
    fun `LoadableData errorCallback works`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/wrong-url', {errorCallback: () => window.myVar = 'Error'})")
        assertThat(driver.checkWindow("ld.loadError.code === 404")).isTrue()
        assertThat(driver.checkWindow("myVar === 'Error'")).isTrue()
    }

    @Test
    fun `LoadableData instance can refresh itself`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld", "new LoadableData('/api/users')")
        driver.createMaryOnBackend()
        driver.executeScript("window.ld.refresh()")
        driver.waitForCondition("ld.refreshing === false")
        assertThat(driver.checkWindow("ld.data.length === 2")).isTrue()
    }

    @Test
    fun `LoadableData instance can refresh instances with same URL`() = TestUtil.test(loadableDataTestApp()) { app, http ->
        driver.get(http.origin + "/ld")
        driver.createLoadableData("ld1", "new LoadableData('/api/users')")
        driver.createLoadableData("ld2", "new LoadableData('/api/users')")
        driver.createMaryOnBackend()
        driver.executeScript("window.ld1.refreshAll()")
        driver.waitForCondition("ld1.refreshing === false") // refresh sets this to true, then false after load
        driver.waitForCondition("ld1.refreshing === false") // refresh sets this to true, then false after load
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
        driver.waitForCondition("ld1.refreshing === false") // refresh sets this to true, then false after load
        driver.waitForCondition("ld1.refreshing === false") // refresh sets this to true, although here it will remain false
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
