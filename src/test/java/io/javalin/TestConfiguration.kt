/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.TestUtil
import org.junit.Test

class TestConfiguration {

    @Test(expected = IllegalStateException::class)
    fun `Javalin#server() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.server { }
    }

    @Test(expected = IllegalStateException::class)
    fun `Javalin#servlet() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.servlet { }
    }

    @Test(expected = IllegalStateException::class)
    fun `Javalin#wsServlet() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.wsServlet { }
    }

    @Test(expected = IllegalStateException::class)
    fun `Javalin#configure() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.configure { _, _ -> }
    }

}
