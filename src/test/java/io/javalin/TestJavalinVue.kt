/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.rendering.vue.JavalinVue
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestJavalinVue {

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/vue/:my-param") {
            it.html(JavalinVue.createLayout(localhost = false) // need to use classpath for test
                    .replace("@routeParams", JavalinVue.getParams(it))
                    .replace("@routeComponent", "<test-component></test-component>")
            )
        }
        val response = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(response).contains("""pathParams: {"my-param":"test-path-param"}""")
        assertThat(response).contains("""queryParams: {"qp":["test-query-param"]}""")
        assertThat(response).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(response).contains("<body><test-component></test-component></body>")
    }

}

