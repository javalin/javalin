/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import io.javalin.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.io.path.Path

class TestStaticFilesEdgeCases {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `server doesn't start for non-existent classpath folder`() = TestUtil.runLogLess {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add("classpath-fake-folder", Location.CLASSPATH) }.start(0) }
            .withMessageContaining("Static resource directory with path: 'classpath-fake-folder' does not exist.")
    }

    @Test
    fun `server doesn't start for non-existent external folder`() = TestUtil.runLogLess {
        val workingDirectory = Path(System.getProperty("user.dir"))
        val fullExternalFakeFolderPath = workingDirectory.resolve("external-fake-folder")
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add("external-fake-folder", Location.EXTERNAL) }.start(0) }
            .withMessageContaining("Static resource directory with path: '$fullExternalFakeFolderPath' does not exist.")
    }

    @Test
    fun `server doesn't start for empty classpath folder`() = TestUtil.runLogLess {
        assertThatExceptionOfType(RuntimeException::class.java)
            .isThrownBy { Javalin.create { it.staticFiles.add(workingDirectory.absolutePath, Location.CLASSPATH) }.start(0) }
            .withMessageContaining("Static resource directory with path: '${workingDirectory.absolutePath}' does not exist.")
            .withMessageContaining("Depending on your setup, empty folders might not get copied to classpath.")
    }

    @Test
    fun `server starts for empty external folder`() = TestUtil.runLogLess {
        Javalin.create { it.staticFiles.add(workingDirectory.absolutePath, Location.EXTERNAL) }.start(0).stop()
    }

    @Test
    fun `test FileUtil`() {
        assertThat(FileUtil.readFile("src/test/external/html.html")).contains("<h1>HTML works</h1>")
        assertThat(FileUtil.readResource("/public/html.html")).contains("<h1>HTML works</h1>")
    }

}
