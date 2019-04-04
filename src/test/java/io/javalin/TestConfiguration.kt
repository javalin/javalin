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
        app.server { it.contextPath = "/test" }
    }

    @Test(expected = IllegalStateException::class)
    fun `Javalin#servlet() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.servlet { it.dynamicGzip = false }
    }

    @Test(expected = IllegalStateException::class)
    fun `Javalin#configure() throws if used after Javalin#start()`() = TestUtil.test { app, http ->
        app.configure { server, servlet ->
            server.contextPath = "/test"
            servlet.dynamicGzip = false
        }
    }

}
