/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestRequest_KotlinSpecific : _UnirestBaseTest() {

    @Test
    fun test_mapQueryParams_worksForGoodInput() {
        _UnirestBaseTest.app.get("/") { ctx ->
            val (name, email, phone) = ctx.mapQueryParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(_UnirestBaseTest.GET_body("/?name=some%20name&email=some%20email&phone=some%20phone"), `is`("some name|some email|some phone"))
    }

    @Test
    fun test_mapQueryParams_isNullForBadInput() {
        _UnirestBaseTest.app.get("/") { ctx ->
            val (missing) = ctx.mapQueryParams("missing") ?: throw IllegalArgumentException()
            ctx.result(missing)
        }
        assertThat(_UnirestBaseTest.GET_body("/?name=some%20name"), `is`("Internal server error"))
    }

}
