/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.misc.TypedException
import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestExceptionMapper : BaseTest() {

    @Test
    fun test_unmappedException_caughtByGeneralHandler() {
        app.get("/unmapped-exception") { ctx -> throw Exception() }
        assertThat(http.get("/unmapped-exception").code(), `is`(500))
        assertThat(http.getBody("/unmapped-exception"), `is`("Internal server error"))
    }

    @Test
    fun test_mappedException_isHandled() {
        app.get("/mapped-exception") { ctx -> throw Exception() }
                .exception(Exception::class.java) { e, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-exception").code(), `is`(200))
        assertThat(http.getBody("/mapped-exception"), `is`("It's been handled."))
    }

    @Test
    fun test_typedMappedException_isHandled() {
        app.get("/typed-exception") { ctx -> throw TypedException() }
                .exception(TypedException::class.java) { e, ctx -> ctx.result(e.proofOfType()) }
        assertThat(http.get("/typed-exception").code(), `is`(200))
        assertThat(http.getBody("/typed-exception"), `is`("I'm so typed"))
    }

    @Test
    fun test_moreSpecificException_isHandledFirst() {
        app.get("/exception-priority") { ctx -> throw TypedException() }
                .exception(Exception::class.java) { e, ctx -> ctx.result("This shouldn't run") }
                .exception(TypedException::class.java) { e, ctx -> ctx.result("Typed!") }
        assertThat(http.get("/exception-priority").code(), `is`(200))
        assertThat(http.getBody("/exception-priority"), `is`("Typed!"))
    }

}
