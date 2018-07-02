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
    fun test_nonExistent_classpathFolder_fails() {
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("some-fake-folder"))
    }

    @Test
    fun test_nonExistent_externalFolder_fails() {
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("some-fake-folder", Location.EXTERNAL))
    }

    @Test
    fun test_empty_classpathFolder_fails() {
        File("src/test/external/empty").mkdir()
        doTest(failureExpected = true, app = Javalin.create().enableStaticFiles("src/test/external/empty", Location.CLASSPATH))
    }

    @Test
    fun test_empty_externalFolder_works() {
        File("src/test/external/empty").mkdir()
        doTest(failureExpected = false, app = Javalin.create().enableStaticFiles("src/test/external/empty", Location.EXTERNAL))
    }

    private fun doTest(failureExpected: Boolean, app: Javalin) {
        var failed = false
        app.event(JavalinEvent.SERVER_START_FAILED) { failed = true }.start().stop()
        assertThat(failed, `is`(failureExpected))
    }

}
