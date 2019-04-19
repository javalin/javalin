/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.FileUtil
import io.javalin.http.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class TestStaticFilesEdgeCases {

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for non-existent classpath folder`() {
        Javalin.create { it.addStaticFiles("some-fake-folder") }.start()
    }

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for non-existent external folder`() {
        Javalin.create { it.addStaticFiles("some-fake-folder", Location.EXTERNAL) }.start()
    }

    @Test(expected = RuntimeException::class)
    fun `server doesn't start for empty classpath folder`() {
        File("src/test/external/empty").mkdir()
        Javalin.create { it.addStaticFiles("src/test/external/empty", Location.CLASSPATH) }.start()
    }

    @Test
    fun `server starts for empty external folder`() {
        File("src/test/external/empty").mkdir()
        Javalin.create { it.addStaticFiles("src/test/external/empty", Location.EXTERNAL) }.start(0).stop()
    }

    @Test
    fun `test FileUtil`() {
        assertThat(FileUtil.readFile("src/test/external/html.html")).contains("<h1>HTML works</h1>")
        assertThat(FileUtil.readResource("/public/html.html")).contains("<h1>HTML works</h1>")
    }

}
