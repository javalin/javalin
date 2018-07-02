/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.BaseTest
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not

class TestHaltException : BaseTest() {

    @Test
    fun test_haltBeforeWildcard_works() {
        app.before("/admin/*") { ctx -> throw HaltException(401) }
        app.get("/admin/protected") { ctx -> ctx.result("Protected resource") }
        assertThat(http.get("/admin/protected").code(), `is`(401))
        assertThat(http.getBody("/admin/protected"), not("Protected resource"))
    }

    @Test
    fun test_haltInRoute_works() {
        app.get("/some-route") { ctx -> throw HaltException(401, "Stop!") }
        assertThat(http.get("/some-route").code(), `is`(401))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

    @Test
    fun test_afterRuns_afterHalt() {
        app.get("/some-route") { ctx -> throw HaltException(401, "Stop!") }.after { ctx -> ctx.status(418) }
        assertThat(http.get("/some-route").code(), `is`(418))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

    @Test
    fun test_constructorsWork() {
        val haltException1 = HaltException()
        val haltException2 = HaltException(401)
        val haltException3 = HaltException("Body")
        val haltException4 = HaltException(401, "Body")
    }

}
