/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.staticfiles.Location
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import java.io.File

class TestStaticFilesEdgeCases {

    @Test
    fun `server doesn't start for non-existent classpath folder`() {
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("some-fake-folder"))
    }

    @Test
    fun `server doesn't start for non-existent external folder`() {
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("some-fake-folder", Location.EXTERNAL))
    }

    @Test
    fun `server doesn't start for empty classpath folder`() {
        File("src/test/external/empty").mkdir()
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("src/test/external/empty", Location.CLASSPATH))
    }

    @Test
    fun `server starts for empty external folder`() {
        File("src/test/external/empty").mkdir()
        doTest(failureExpected = false, app = Javalin.create().enableStaticFiles("src/test/external/empty", Location.EXTERNAL))
    }

    private fun doTest(failureExpected: Boolean, app: Javalin) {
        var failed = false
        app.event(JavalinEvent.SERVER_START_FAILED) { failed = true }.start(0).stop()
        assertThat(failed, `is`(failureExpected))
    }

}
