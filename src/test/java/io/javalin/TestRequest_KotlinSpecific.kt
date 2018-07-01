/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestRequest_KotlinSpecific : _UnirestBaseTest() {

    @Test
    fun test_mapQueryParams_worksForGoodInput() {
        app.get("/") { ctx ->
            val (name, email, phone) = ctx.mapQueryParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(Unirest.get(_UnirestBaseTest.origin + "/?name=some%20name&email=some%20email&phone=some%20phone").asString().body, `is`("some name|some email|some phone"))
    }

    @Test
    fun test_mapQueryParams_isNullForBadInput() {
        app.get("/") { ctx ->
            val (name, missing) = ctx.mapQueryParams("name", "missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        assertThat(Unirest.get(_UnirestBaseTest.origin + "/?name=some%20name").asString().body, `is`("Internal server error"))
    }

    @Test
    fun test_mapFormParams_worksForGoodInput() {
        app.post("/") { ctx ->
            val (name, email, phone) = ctx.mapFormParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        val response = Unirest.post(_UnirestBaseTest.origin).body("name=some%20name&email=some%20email&phone=some%20phone").asString()
        assertThat(response.body, `is`("some name|some email|some phone"))
    }

    @Test
    fun test_mapFormParams_isNullForBadInput() {
        app.post("/") { ctx ->
            val (name, missing) = ctx.mapFormParams("missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        val response = Unirest.post(_UnirestBaseTest.origin).body("name=some%20name").asString()
        assertThat(response.body, `is`("Internal server error"))
    }

}
