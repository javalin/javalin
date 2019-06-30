/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.rendering.vue.JavalinVue
import io.javalin.plugin.rendering.vue.VueComponent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestJavalinVue {

    data class User(val name: String, val email: String)
    data class Role(val name: String)
    data class State(val user: User, val role: Role)

    private val state = State(User("tipsy", "tipsy@tipsy.tipsy"), Role("Maintainer"))

    @Test
    fun `hello vue world`() = TestUtil.test { app, http ->
        JavalinVue.localPath = "src/test/resources/vue"
        JavalinVue.stateFunction = { ctx -> state }
        app.get("/vue/:my-param", VueComponent("<test-component></test-component>"))
        val stateResponse = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(stateResponse).contains("""pathParams: {"my-param":"test-path-param"}""")
        assertThat(stateResponse).contains("""queryParams: {"qp":["test-query-param"]}""")
        assertThat(stateResponse).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(stateResponse).contains("""state: {"user":{"name":"tipsy","email":"tipsy@tipsy.tipsy"},"role":{"name":"Maintainer"}}""")
        assertThat(stateResponse).contains("<body><test-component></test-component></body>")
        JavalinVue.stateFunction = { ctx -> mapOf<String, String>() }
        app.get("/no-state", VueComponent("<test-component></test-component>"))
        val noStateResponse = http.getBody("/no-state")
        assertThat(noStateResponse).contains("""pathParams: {}""")
        assertThat(noStateResponse).contains("""queryParams: {}""")
        assertThat(noStateResponse).contains("""state: {}""")
    }

}
