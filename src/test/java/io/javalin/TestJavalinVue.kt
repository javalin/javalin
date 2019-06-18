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

    @Test
    fun `hello vue world`() = TestUtil.test { app, http ->
        JavalinVue.localPath = "src/test/resources/vue"
        app.get("/vue/:my-param", VueComponent("<test-component></test-component>"))
        val response = http.getBody("/vue/test-path-param?qp=test-query-param")
        assertThat(response).contains("""pathParams: {"my-param":"test-path-param"}""")
        assertThat(response).contains("""queryParams: {"qp":["test-query-param"]}""")
        assertThat(response).contains("""Vue.component("test-component", {template: "#test-component"});""")
        assertThat(response).contains("<body><test-component></test-component></body>")
    }

}
