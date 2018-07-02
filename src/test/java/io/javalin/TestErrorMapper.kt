/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestErrorMapper : BaseTest() {

    @Test
    fun test_404mapper_works() {
        app.error(404) { ctx -> ctx.result("Custom 404 page") }
        assertThat(http.getBody("/unmapped"), `is`("Custom 404 page"))
    }

    @Test
    fun test_500mapper_works() {
        app.get("/exception") { ctx -> throw RuntimeException() }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun testError_higherPriority_thanException() {
        app.get("/exception") { ctx -> throw RuntimeException() }
                .exception(Exception::class.java) { e, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun testError_throwingException_isCaughtByExceptionMapper() {
        app.get("/exception") { ctx -> throw RuntimeException() }
                .exception(Exception::class.java) { e, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx ->
                    ctx.result("Custom 500 page")
                    throw RuntimeException()
                }
        assertThat(http.getBody("/exception"), `is`("Exception handled!"))
    }

}
