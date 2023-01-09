/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.javalinvue

import io.javalin.http.Context
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.staticfiles.Location
import io.javalin.vue.VueComponent
import io.javalin.vue.VueRenderer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.util.URIUtil
import org.junit.jupiter.api.Test
import java.nio.file.Paths

class TestJavalinVue {

    data class User(val name: String, val email: String)
    data class Role(val name: String)
    data class State(val user: User, val role: Role)

    private val state = State(User("tipsy", "tipsy@tipsy.tipsy"), Role("Maintainer"))

    private fun String.uriEncodeForJavascript() =
        URIUtil.encodePath(this)

    @Test
    fun `vue component with state`() = VueTestUtil.test({
        it.vue.stateFunction = { state }
    }) { app, http ->
        val encodedState = """{"pathParams":{"my-param":"test-path-param"},"state":{"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}}""".uriEncodeForJavascript()
        app.get("/vue/{my-param}", VueComponent("test-component"))
        val res = http.getBody("/vue/test-path-param")
        assertThat(res).contains(encodedState)
        assertThat(res).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
    }

    @Test
    fun `vue component without state`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component"))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
    }

    @Test
    fun `vue component without state with pre renderer`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component", null, object : VueRenderer() {
            override fun preRender(layout: String, ctx: Context): String {
                return layout.plus("PRE_RENDER");
            }
        }))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
        assertThat(res).contains("PRE_RENDER")
        assertThat(res).doesNotContain("POST_RENDER")
    }

    @Test
    fun `vue component without state with post renderer`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component", null, object : VueRenderer() {
            override fun postRender(layout: String, ctx: Context): String {
                return layout + "POST_RENDER";
            }
        }))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
        assertThat(res).doesNotContain("PRE_RENDER")
        assertThat(res).contains("POST_RENDER")
    }

    @Test
    fun `vue component without state with default renderer`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component", VueRenderer()))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
        assertThat(res).doesNotContain("PRE_RENDER")
        assertThat(res).doesNotContain("POST_RENDER")
    }

    @Test
    fun `vue component without state with pre and post renderer`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component", null, object : VueRenderer() {
            override fun postRender(layout: String, ctx: Context): String {
                return layout + "POST_RENDER";
            }

            override fun preRender(layout: String, ctx: Context): String {
                return layout + "PRE_RENDER";
            }
        }))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component></test-component></body>")
        assertThat(res).contains("Vue.prototype.\$javalin")
        assertThat(res).contains("POST_RENDER")
        assertThat(res).contains("PRE_RENDER")
    }

    @Test
    fun `vue3 component without state`() = VueTestUtil.test({
        it.vue.vueAppName = "app"
    }) { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component-3"))
        val res = http.getBody("/no-state")
        assertThat(res).contains(encodedEmptyState)
        assertThat(res).contains("<body><test-component-3></test-component-3></body>")
        assertThat(res).contains("app.config.globalProperties.\$javalin")
    }


    @Test
    fun `vue3 component with state`() = VueTestUtil.test({
        it.vue.vueAppName = "app"
        it.vue.stateFunction = { state }
    }) { app, http ->
        val encodedState = """{"pathParams":{"my-param":"test-path-param"},"state":{"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}}""".uriEncodeForJavascript()
        app.get("/vue/{my-param}", VueComponent("test-component-3"))
        val res = http.getBody("/vue/test-path-param")
        assertThat(res).contains(encodedState)
        assertThat(res).contains("""app.component("test-component-3", {template: "#test-component-3"});""")
        assertThat(res).contains("<body><test-component-3></test-component-3></body>")
        assertThat(res).contains("app.config.globalProperties.\$javalin")
    }

    @Test
    fun `vue component with component-specific state`() = VueTestUtil.test { app, http ->
        val encodedEmptyState = """{"pathParams":{},"state":{}}""".uriEncodeForJavascript()
        val encodedTestState = """{"pathParams":{},"state":{"test":"tast"}}""".uriEncodeForJavascript()
        app.get("/no-state", VueComponent("test-component"))
        val noStateRes = http.getBody("/no-state")
        app.get("/specific-state", VueComponent("test-component", mapOf("test" to "tast")))
        val specificStateRes = http.getBody("/specific-state")
        assertThat(noStateRes).contains(encodedEmptyState)
        assertThat(specificStateRes).contains(encodedTestState)
    }

    @Test
    fun `vue component works Javalin#error`() = VueTestUtil.test { app, http ->
        app.get("/") { it.status(NOT_FOUND) }
        app.error(NOT_FOUND, "html", VueComponent("test-component"))
        assertThat(http.htmlGet("/").body).contains("<body><test-component></test-component></body>")
    }

    @Test
    fun `unicode in template works`() = VueTestUtil.test { app, http ->
        app.get("/unicode", VueComponent("test-component"))
        assertThat(http.getBody("/unicode")).contains("<div>Test ÆØÅ</div>")
    }

    @Test
    fun `state is escaped`() = VueTestUtil.test { app, http ->
        val encodedXSS = "%3Cscript%3Ealert(1)%3Cscript%3E"
        app.get("/escaped", VueComponent("test-component", mapOf("xss" to "<script>alert(1)<script>")))
        assertThat(http.getBody("/escaped")).doesNotContain("<script>alert(1)<script>")
        assertThat(http.getBody("/escaped")).contains(encodedXSS)
    }

    @Test
    fun `component shorthand works`() = VueTestUtil.test { app, http ->
        app.get("/shorthand", VueComponent("test-component"))
        assertThat(http.getBody("/shorthand")).contains("<test-component></test-component>")
    }

    @Test
    fun `non-existent component fails`() = VueTestUtil.test({
        it.vue.optimizeDependencies = false
    }) { app, http ->
        app.get("/fail", VueComponent("unknown-component"))
        assertThat(http.getBody("/fail")).contains("Route component not found: <unknown-component></unknown-component>")
    }

    @Test
    fun `component can have attributes`() = VueTestUtil.test { app, http ->
        app.get("/attr", VueComponent("<test-component attr='1'></test-component>"))
        assertThat(http.getBody("/attr")).contains("<test-component attr='1'>")
    }

    @Test
    fun `classpath rootDirectory works`() = VueTestUtil.test({
        it.vue.rootDirectory("/vue")
    }) { app, http ->
        app.get("/classpath", VueComponent("test-component"))
        assertThat(http.getBody("/classpath")).contains("<test-component></test-component>")
    }

    @Test
    fun `setting rootDirectory with Path works`() = VueTestUtil.test({
        it.vue.rootDirectory(Paths.get("src/test/resources/vue"))
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("<test-component></test-component>")
    }

    @Test
    fun `non-existent folder fails`() = VueTestUtil.test({
        it.vue.rootDirectory("/vue", Location.EXTERNAL)
    }) { app, http ->
        app.get("/fail", VueComponent("test-component"))
        assertThat(http.get("/fail").status).isEqualTo(500)
    }


    @Test
    fun `@cdnWebjar resolves to webjar on localhost`() = VueTestUtil.test({
        it.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
        it.contextResolver.url = { "http://localhost:1234/" }
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("""src="/webjars/""")
    }

    @Test
    fun `@cdnWebjar resolves to cdn on non-localhost`() = VueTestUtil.test({
        it.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
        it.contextResolver.url = { "https://example.com" }
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("""src="https://cdn.jsdelivr.net/webjars/""")
    }

    @Test
    fun `@cdnWebjar resolves to https even on non https hosts`() = VueTestUtil.test({
        it.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
        it.contextResolver.url = { "http://123.123.123.123:1234/" }
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("""src="https://cdn.jsdelivr.net/webjars/""")
    }

    @Test
    fun `@inlineFile functionality works as expected if not-dev`() = VueTestUtil.test({
        it.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
        it.contextResolver.url = { "http://123.123.123.123:1234/" }
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        val responseBody = http.getBody("/path")
        assertThat(responseBody).contains("""<script>let a = "Always included";let ${"\$"}a = "Dollar works"</script>""")
        assertThat(responseBody).contains("""<script>let b = "Included if not dev"</script>""")
        assertThat(responseBody).doesNotContain("""<script>let b = "Included if dev"</script>""")
        assertThat(responseBody).doesNotContain("""<script>@inlineFileDev("/vue/scripts-dev.js")</script>""")
        assertThat(responseBody).doesNotContain("""<script>@inlineFile""")
    }

    @Test
    fun `@inlineFile functionality works as expected if dev`() = VueTestUtil.test({
        it.vue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
        it.contextResolver.url = { "http://localhost:1234/" }
    }) { app, http ->
        app.get("/path", VueComponent("test-component"))
        val responseBody = http.getBody("/path")
        assertThat(responseBody).contains("""<script>let a = "Always included";let ${"\$"}a = "Dollar works"</script>""")
        assertThat(responseBody).contains("""<script>let b = "Included if dev"</script>""")
        assertThat(responseBody).doesNotContain("""<script>let b = "Included if not dev"</script>""")
        assertThat(responseBody).doesNotContain("""<script>@inlineFileNotDev("/vue/scripts-not-dev.js")</script>""")
        assertThat(responseBody).doesNotContain("""<script>@inlineFile""")
    }

    @Test
    fun `LoadableData class is included`() = VueTestUtil.test { app, http ->
        app.get("/shorthand", VueComponent("test-component"))
        val response = http.getBody("/shorthand")
        assertThat(response).contains("LoadableData")
    }

}
