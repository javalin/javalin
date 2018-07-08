/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.misc.TypedException
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestExceptionMapper {

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, http ->
        app.get("/unmapped-exception") { ctx -> throw Exception() }
        assertThat(http.get("/unmapped-exception").status, `is`(500))
        assertThat(http.getBody("/unmapped-exception"), `is`("Internal server error"))
    }

    @Test
    fun `mapped exceptions are handled`() = TestUtil.test { app, http ->
        app.get("/mapped-exception") { ctx -> throw Exception() }
                .exception(Exception::class.java) { e, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-exception").status, `is`(200))
        assertThat(http.getBody("/mapped-exception"), `is`("It's been handled."))
    }

    @Test
    fun `type information of exception is not lost`() = TestUtil.test { app, http ->
        app.get("/typed-exception") { ctx -> throw TypedException() }
                .exception(TypedException::class.java) { e, ctx -> ctx.result(e.proofOfType()) }
        assertThat(http.get("/typed-exception").status, `is`(200))
        assertThat(http.getBody("/typed-exception"), `is`("I'm so typed"))
    }

    @Test
    fun `most specific exception handler handles exception`() = TestUtil.test { app, http ->
        app.get("/exception-priority") { ctx -> throw TypedException() }
                .exception(Exception::class.java) { e, ctx -> ctx.result("This shouldn't run") }
                .exception(TypedException::class.java) { e, ctx -> ctx.result("Typed!") }
        assertThat(http.get("/exception-priority").status, `is`(200))
        assertThat(http.getBody("/exception-priority"), `is`("Typed!"))
    }

}
