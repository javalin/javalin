/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.ApiBuilder.get
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class TestApiBuilderTwoServices {

    @Test
    fun testApiBuilder_twoServices() {
        val app1 = Javalin.create().port(0).start()
        val app2 = Javalin.create().port(0).start()
        app1.routes { get("/hello-1") { ctx -> ctx.result("Hello-1") } }
        app1.routes { get("/hello-2") { ctx -> ctx.result("Hello-2") } }
        app2.routes { get("/hello-1") { ctx -> ctx.result("Hello-1") } }
        app2.routes { get("/hello-2") { ctx -> ctx.result("Hello-2") } }
        assertThat(Unirest.get("http://localhost:" + app1.port() + "/hello-1").asString().body, `is`("Hello-1"))
        assertThat(Unirest.get("http://localhost:" + app1.port() + "/hello-2").asString().body, `is`("Hello-2"))
        assertThat(Unirest.get("http://localhost:" + app2.port() + "/hello-1").asString().body, `is`("Hello-1"))
        assertThat(Unirest.get("http://localhost:" + app2.port() + "/hello-2").asString().body, `is`("Hello-2"))
        app1.stop()
        app2.stop()
    }

}
