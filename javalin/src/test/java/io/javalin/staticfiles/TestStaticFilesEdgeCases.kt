/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.core.util.FileUtil
import io.javalin.core.util.JavalinLogger
import io.javalin.http.staticfiles.Location
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestStaticFilesEdgeCases {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `server doesn't start for non-existent classpath folder`() {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.addStaticFiles("classpath-fake-folder", Location.CLASSPATH) }.start() }
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
            .isThrownBy { Javalin.create { it.addStaticFiles(workingDirectory.absolutePath, Location.CLASSPATH) }.start() }
            .withMessageStartingWith("Static resource directory with path: '${workingDirectory.absolutePath}' does not exist.")
            .withMessageEndingWith("Depending on your setup, empty folders might not get copied to classpath.")
    }

    @Test
    fun `server starts for empty external folder`() {
        JavalinLogger.enabled = false
        Javalin.create { it.addStaticFiles(workingDirectory.absolutePath, Location.EXTERNAL) }.start(0).stop()
        JavalinLogger.enabled = true
    }

    @Test
    fun `test FileUtil`() {
        assertThat(FileUtil.readFile("src/test/external/html.html")).contains("<h1>HTML works</h1>")
        assertThat(FileUtil.readResource("/public/html.html")).contains("<h1>HTML works</h1>")
    }

}
