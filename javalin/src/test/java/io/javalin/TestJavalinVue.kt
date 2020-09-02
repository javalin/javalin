/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.file.Paths

class TestJavalinVue {

    init {
        JavalinVue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
    }

    data class User(val name: String, val email: String)
    data class Role(val name: String)
    data class State(val user: User, val role: Role)

    private val state = State(User("tipsy", "tipsy@tipsy.tipsy"), Role("Maintainer"))

    @Test
    fun `vue component with state`() = TestUtil.test { app, http ->
        JavalinVue.stateFunction = { ctx -> state }
        app.get("/vue/:my-param", VueComponent("<test-component></test-component>"))
        val res = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(res).contains("""
                |    Vue.prototype.${"$"}javalin = {
                |        pathParams: {"my-param":"test-path-param"},
                |        queryParams: {"qp":["test-query-param"]},
                |        state: {"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}
                |    }""".trimMargin())
        assertThat(res).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(res).contains("<body><test-component></test-component></body>")
    }

    @Test
    fun `vue component without state`() = TestUtil.test { app, http ->
        JavalinVue.stateFunction = { ctx -> mapOf<String, String>() }
        app.get("/no-state", VueComponent("<test-component></test-component>"))
        val res = http.getBody("/no-state")
        assertThat(res).contains("""pathParams: {}""")
        assertThat(res).contains("""queryParams: {}""")
        assertThat(res).contains("""state: {}""")
        assertThat(res).contains("<body><test-component></test-component></body>")
    }

    @Test
    fun `vue component with component-specific state`() = TestUtil.test { app, http ->
        JavalinVue.stateFunction = { ctx -> mapOf<String, String>() }
        app.get("/no-state", VueComponent("<test-component></test-component>"))
        val noStateRes = http.getBody("/no-state")
        app.get("/specific-state", VueComponent("<test-component></test-component>", mapOf("test" to "tast")))
        val specificStateRes = http.getBody("/specific-state")
        assertThat(noStateRes).contains("""state: {}""")
        assertThat(specificStateRes).contains("""state: {"test":"tast"}""")
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
        val xss = "%3Cscript%3Ealert%281%29%3Cscript%3E"
        app.get("/escaped", VueComponent("<test-component></test-component>"))
        // keys
        assertThat(http.getBody("/escaped?${xss}=value")).doesNotContain("<script>alert(1)<script>")
        assertThat(http.getBody("/escaped?${xss}=value")).contains("&lt;script&gt;alert(1)&lt;script&gt;")
        // values
        assertThat(http.getBody("/escaped?key=${xss}")).doesNotContain("<script>alert(1)<script>")
        assertThat(http.getBody("/escaped?key=${xss}")).contains("&lt;script&gt;alert(1)&lt;script&gt;")
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
    fun `classpath works`() = TestUtil.test { app, http ->
        JavalinVue.rootDirectory("/vue", Location.CLASSPATH)
        app.get("/classpath", VueComponent("test-component"))
        assertThat(http.getBody("/classpath")).contains("<test-component></test-component>")
        JavalinVue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
    }

    @Test
    fun `setting rootDirectory with Path works`() = TestUtil.test { app, http ->
        JavalinVue.rootDirectory(Paths.get("src/test/resources/vue"))
        app.get("/path", VueComponent("test-component"))
        assertThat(http.getBody("/path")).contains("<test-component></test-component>")
        JavalinVue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
    }

    @Test
    fun `non-existent folder fails`() = TestUtil.test { app, http ->
        JavalinVue.rootDirectory("/vue", Location.EXTERNAL)
        app.get("/fail", VueComponent("test-component"))
        assertThat(http.get("/fail").status).isEqualTo(500)
        JavalinVue.rootDirectory("src/test/resources/vue", Location.EXTERNAL)
    }

    @Test
    fun `webjars uses cdn when tagged with @cdnWebjar to`() = TestUtil.test { app, http ->
        app.get("/unicode", VueComponent("<test-component></test-component>"))
        assertThat(http.getBody("/unicode")).contains("""<script src="/webjars/swagger-ui/3.24.3/swagger-ui.css"></script>""")
    }

}
