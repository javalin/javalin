/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Test

class TestHaltException {

    @Test
    fun `throwing HaltException in before-handler works`() = TestUtil.test { app, http ->
        app.before("/admin/*") { ctx -> throw HaltException(401) }
        app.get("/admin/protected") { ctx -> ctx.result("Protected resource") }
        assertThat(http.get("/admin/protected").code(), `is`(401))
        assertThat(http.getBody("/admin/protected"), not("Protected resource"))
    }

    @Test
    fun `throwing HaltException in endpoint-handler works`() = TestUtil.test { app, http ->
        app.get("/some-route") { ctx -> throw HaltException(401, "Stop!") }
        assertThat(http.get("/some-route").code(), `is`(401))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

    @Test
    fun `after-handlers execute after HaltException`() = TestUtil.test { app, http ->
        app.get("/some-route") { ctx -> throw HaltException(401, "Stop!") }.after { ctx -> ctx.status(418) }
        assertThat(http.get("/some-route").code(), `is`(418))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

}
