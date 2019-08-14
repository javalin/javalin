/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.staticfiles.Location
import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

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
        app.get("/no-state", VueComponent("<other-component></other-component>"))
        val res = http.getBody("/no-state")
        assertThat(res).contains("""pathParams: {}""")
        assertThat(res).contains("""queryParams: {}""")
        assertThat(res).contains("""state: {}""")
        assertThat(res).contains("<body><other-component></other-component></body>")
    }

    @Test
    fun `vue component works Javalin#error`() = TestUtil.test { app, http ->
        app.get("/") { it.status(404) }
        app.error(400, VueComponent("<bad-request></bad-request>"))
        app.error(404, "html", VueComponent("<not-found></not-found>"))
        assertThat(http.htmlGet("/").body).contains("<body><not-found></not-found></body>")
    }

    @Test
    fun `unicode in template works`() = TestUtil.test { app, http ->
        app.get("/unicode", VueComponent("<test-component></test-component>"))
        assertThat(http.getBody("/unicode")).contains("<div>Test ÆØÅ</div>")
    }

    @Test
    fun `component shorthand works`() = TestUtil.test { app, http ->
        app.get("/shorthand", VueComponent("test-component"))
        assertThat(http.getBody("/shorthand")).contains("<test-component></test-component>")
    }

}
