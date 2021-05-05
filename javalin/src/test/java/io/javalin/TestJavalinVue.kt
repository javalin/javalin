/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Context
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import io.javalin.testing.TestUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.nio.file.Paths

class TestJavalinVue {

    @Before
    fun setup() {
        before()
    }

    companion object {
        fun before() {
            with(JavalinVue) {
                vueVersion { it.vue2() }
                isDev = null // reset
                stateFunction = { ctx -> mapOf<String, String>() } // reset
                rootDirectory { it.externalPath("src/test/resources/vue") } // src/main ->
                optimizeDependencies = false
            }
        }
    }

    data class User(val name: String, val email: String)
    data class Role(val name: String)
    data class State(val user: User, val role: Role)

    private val state = State(User("tipsy", "tipsy@tipsy.tipsy"), Role("Maintainer"))

    private fun String.uriEncodeForJavascript() =
            URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

    @Test
    fun `vue component with state`() = TestUtil.test { app, http ->
        val encodedState = """{"pathParams":{"my-param":"test-path-param"},"queryParams":{"qp":["test-query-param"]},"state":{"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}}""".uriEncodeForJavascript()
        JavalinVue.stateFunction = { ctx -> state }
        app.get("/vue/:my-param", VueComponent("<test-component></test-component>"))
        val res = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(res).contains(encodedState)
        assertThat(res).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
    }

    @Test
    fun `vue component without state`() = TestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"queryParams":{},"state":{}}""".uriEncodeForJavascript()

        app.get("/no-state", VueComponent("<test-component></test-component>"))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
    }

    @Test
    fun `vue3 component without state`() = TestUtil.test { app, http ->
        JavalinVue.vueVersion { it.vue3("app") }
        val encodedEmptyState = """{"pathParams":{},"queryParams":{},"state":{}}""".uriEncodeForJavascript()

        app.get("/no-state", VueComponent("<test-component-3></test-component-3>"))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component-3></test-component-3></body>")
        assertThat(res).contains("app.config.globalProperties.\$javalin")
    }


    @Test
    fun `vue3 component with state`() = TestUtil.test { app, http ->
        JavalinVue.vueVersion { it.vue3("app") }
        val encodedState = """{"pathParams":{"my-param":"test-path-param"},"queryParams":{"qp":["test-query-param"]},"state":{"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}}""".uriEncodeForJavascript()
        JavalinVue.stateFunction = { ctx -> state }
        app.get("/vue/:my-param", VueComponent("<test-component-3></test-component-3>"))
        val res = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(res).contains(encodedState)
        assertThat(res).contains("""app.component("test-component-3", {template: "#test-component-3"});""")
        assertThat(res).contains("<body><test-component-3></test-component-3></body>")
        assertThat(res).contains("app.config.globalProperties.\$javalin")
    }

    @Test
    fun `vue component with component-specific state`() = TestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"queryParams":{},"state":{}}""".uriEncodeForJavascript()

        val encodedTestState = """{"pathParams":{},"queryParams":{},"state":{"test":"tast"}}""".uriEncodeForJavascript()

        app.get("/no-state", VueComponent("<test-component></test-component>"))
        val noStateRes = http.getBody("/no-state")
        app.get("/specific-state", VueComponent("<test-component></test-component>", mapOf("test" to "tast")))
        val specificStateRes = http.getBody("/specific-state")
        assertThat(noStateRes).contains(encodedEmptyState)
        assertThat(specificStateRes).contains(encodedTestState)
    }

    @Test
    fun `vue component works Javalin#error`() = TestUtil.test { app, http ->
        app.get("/") { it.status(404) }
        app.error(404, "html", VueComponent("<test-component></test-component>"))
        assertThat(http.htmlGet("/").body).contains("<body><test-component></test-component></body>")
    }

    @Test
    fun `unicode in template works`() = TestUtil.test { app, http ->
        app.get("/unicode", VueComponent("<test-component></test-component>"))
        assertThat(http.getBody("/unicode")).contains("<div>Test ÆØÅ</div>")
    }

    @Test
    fun `default params are escaped`() = TestUtil.test { app, http ->
        val encodedXSS = "%3Cscript%3Ealert%281%29%3Cscript%3E"
        app.get("/escaped", VueComponent("<test-component></test-component>"))
        // keys
        assertThat(http.getBody("/escaped?${encodedXSS}=value")).doesNotContain("<script>alert(1)<script>")
        assertThat(http.getBody("/escaped?${encodedXSS}=value")).contains(encodedXSS)
        // values
        assertThat(http.getBody("/escaped?key=${encodedXSS}")).doesNotContain("<script>alert(1)<script>")
        assertThat(http.getBody("/escaped?key=${encodedXSS}")).contains(encodedXSS)
    }

    @Test
    fun `quotes are handled correctly`() = TestUtil.test { app, http ->
        val encodedTestObject = """"test":["\"cool\""]""".uriEncodeForJavascript()
        app.get("/escaped", VueComponent("<test-component></test-component>"))

        assertThat(http.getBody("""/escaped?test=%22cool%22""")).contains(encodedTestObject)
    }

    @Test
    fun `component shorthand works`() = TestUtil.test { app, http ->
        app.get("/shorthand", VueComponent("test-component"))
        assertThat(http.getBody("/shorthand")).contains("<test-component></test-component>")
    }

    @Test
    fun `non-existent component fails`() = TestUtil.test { app, http ->
        app.get("/fail", VueComponent("unknown-component"))
        assertThat(http.getBody("/fail")).contains("Route component not found: <unknown-component></unknown-component>")
    }

    @Test
    fun `component can have attributes`() = TestUtil.test { app, http ->
        app.get("/attr", VueComponent("<test-component attr='1'></test-component>"))
        assertThat(http.getBody("/attr")).contains("<test-component attr='1'>")
    }

    @Test
    fun `classpath rootDirectory works`() = TestUtil.test { app, http ->
        JavalinVue.rootDirectory { it.classpathPath("/vue") }
        app.get("/classpath", VueComponent("test-component"))
        assertThat(http.getBody("/classpath")).contains("<test-component></test-component>")
    }

    @Test
    fun `setting rootDirectory with Path works`() = TestUtil.test { app, http ->
        JavalinVue.rootDirectory = Paths.get("src/test/resources/vue")
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("<test-component></test-component>")
    }

    @Test
    fun `non-existent folder fails`() = TestUtil.test { app, http ->
        JavalinVue.isDev = true // reset
        JavalinVue.rootDirectory { it.externalPath("/vue") }
        app.get("/fail", VueComponent("test-component"))
        assertThat(http.get("/fail").status).isEqualTo(500)
    }

    @Test
    fun `@cdnWebjar resolves to webjar on localhost`() = TestUtil.test { app, http ->
        val ctx = mockk<Context>(relaxed = true)
        JavalinVue.isDev = true // reset
        every { ctx.url() } returns "http://localhost:1234/"
        VueComponent("<test-component></test-component>").handle(ctx)
        val slot = slot<String>().also { verify { ctx.html(html = capture(it)) } }
        assertThat(slot.captured).contains("""src="/webjars/""")
    }

    @Test
    fun `@cdnWebjar resolves to cdn on non-localhost`() = TestUtil.test { app, http ->
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.url() } returns "https://example.com"
        VueComponent("<test-component></test-component>").handle(ctx)
        val slot = slot<String>().also { verify { ctx.html(html = capture(it)) } }
        assertThat(slot.captured).contains("""src="https://cdn.jsdelivr.net/webjars/""")
    }

    @Test
    fun `@cdnWebjar resolves to https even on non https hosts`() = TestUtil.test { app, http ->
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.url() } returns "http://123.123.123.123:1234/"
        VueComponent("<test-component></test-component>").handle(ctx)
        val slot = slot<String>().also { verify { ctx.html(html = capture(it)) } }
        assertThat(slot.captured).contains("""src="https://cdn.jsdelivr.net/webjars/""")
    }

    @Test
    fun `@inlineFile functionality works as expected if not-dev`() = TestUtil.test { app, http ->
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.url() } returns "http://123.123.123.123:1234/"
        VueComponent("<test-component></test-component>").handle(ctx)
        val slot = slot<String>().also { verify { ctx.html(html = capture(it)) } }
        assertThat(slot.captured).contains("""<script>let a = "Always included";let ${"\$"}a = "Dollar works"</script>""")
        assertThat(slot.captured).contains("""<script>let b = "Included if not dev"</script>""")
        assertThat(slot.captured).doesNotContain("""<script>let b = "Included if dev"</script>""")
        assertThat(slot.captured).doesNotContain("""<script>@inlineFileDev("/vue/scripts-dev.js")</script>""")
        assertThat(slot.captured).doesNotContain("""<script>@inlineFile""")
    }

    @Test
    fun `@inlineFile functionality works as expected if dev`() = TestUtil.test { app, http ->
        val ctx = mockk<Context>(relaxed = true)
        every { ctx.url() } returns "http://localhost:1234/"
        VueComponent("<test-component></test-component>").handle(ctx)
        val slot = slot<String>().also { verify { ctx.html(html = capture(it)) } }
        assertThat(slot.captured).contains("""<script>let a = "Always included";let ${"\$"}a = "Dollar works"</script>""")
        assertThat(slot.captured).contains("""<script>let b = "Included if dev"</script>""")
        assertThat(slot.captured).doesNotContain("""<script>let b = "Included if not dev"</script>""")
        assertThat(slot.captured).doesNotContain("""<script>@inlineFileNotDev("/vue/scripts-not-dev.js")</script>""")
        assertThat(slot.captured).doesNotContain("""<script>@inlineFile""")
    }

}
