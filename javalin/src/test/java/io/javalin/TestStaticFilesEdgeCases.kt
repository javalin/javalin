/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.FileUtil
import io.javalin.http.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import java.io.File

class TestStaticFilesEdgeCases {

    @Test
    fun `server doesn't start for non-existent classpath folder`() {
        assertThatExceptionOfType(RuntimeException::class.java)
                .isThrownBy { Javalin.create { it.addStaticFiles("classpath-fake-folder") }.start() }
                .withMessageStartingWith("Static resource directory with path: 'classpath-fake-folder' does not exist.")
    }

    @Test
    fun `server doesn't start for non-existent external folder`() {
        assertThatExceptionOfType(RuntimeException::class.java)
                .isThrownBy { Javalin.create { it.addStaticFiles("external-fake-folder", Location.EXTERNAL) }.start() }
                .withMessageStartingWith("Static resource directory with path: 'external-fake-folder' does not exist.")
    }

    @Test
    fun `server doesn't start for empty classpath folder`() {
        assertThatExceptionOfType(RuntimeException::class.java)
                .isThrownBy {
                    File("src/test/external/empty").mkdir()
                    Javalin.create { it.addStaticFiles("src/test/external/empty", Location.CLASSPATH) }.start()
                }
                .withMessageStartingWith("Static resource directory with path: 'src/test/external/empty' does not exist.")
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
