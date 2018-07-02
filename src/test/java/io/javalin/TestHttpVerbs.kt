/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.util.BaseTest
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`

class TestHttpVerbs : BaseTest() {

    @Test
    fun test_get_helloWorld() {
        app.get("/hello") { ctx -> ctx.result("Hello World") }
        assertThat(http.getBody("/hello"), `is`("Hello World"))
    }

    @Test
    fun test_get_helloOtherWorld() {
        app.get("/hello") { ctx -> ctx.result("Hello New World") }
        assertThat(http.getBody("/hello"), `is`("Hello New World"))
    }

    @Test
    fun test_all_mapped_verbs_ok() {
        app.get("/mapped", okHandler)
        app.post("/mapped", okHandler)
        app.put("/mapped", okHandler)
        app.delete("/mapped", okHandler)
        app.patch("/mapped", okHandler)
        app.head("/mapped", okHandler)
        app.options("/mapped", okHandler)
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/mapped").status, `is`(200))
        }
    }

    @Test
    fun test_all_unmapped_verbs_ok() {
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/unmapped").status, `is`(404))
        }
    }

    @Test
    fun test_headOk_ifGetMapped() {
        app.get("/mapped", okHandler)
        assertThat(http.call(HttpMethod.HEAD, "/mapped").status, `is`(200))
    }

    @Test
    fun test_filterOrder_preserved() {
        app.before { ctx -> ctx.result("1") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "2") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "3") }
        app.before { ctx -> ctx.result(ctx.resultString()!! + "4") }
        app.get("/hello") { ctx -> ctx.result(ctx.resultString()!! + "Hello") }
        assertThat(http.getBody("/hello"), `is`("1234Hello"))
    }

}
