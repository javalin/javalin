/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.staticfiles.Location
import org.junit.Test
import java.io.File

class TestStaticFilesEdgeCases {

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for non-existent classpath folder`() {
        Javalin.create().configure { it.addStaticFiles("some-fake-folder") }.start()
    }

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for non-existent external folder`() {
        Javalin.create().configure { it.addStaticFiles("some-fake-folder", Location.EXTERNAL) }.start()
    }

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for empty classpath folder`() {
        File("src/test/external/empty").mkdir()
        Javalin.create().configure { it.addStaticFiles("src/test/external/empty", Location.CLASSPATH) }.start()
    }

    @Test
    fun `server starts for empty external folder`() {
        File("src/test/external/empty").mkdir()
        Javalin.create().configure { it.addStaticFiles("src/test/external/empty", Location.EXTERNAL) }.start(0).stop()
    }

}
