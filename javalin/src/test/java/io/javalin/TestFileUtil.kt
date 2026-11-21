/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

class TestFileUtil {

    @Test
    fun `streamToFile accepts a filename without a parent directory`() {
        val file = File("javalin-fileutil-${UUID.randomUUID()}.txt")
        try {
            FileUtil.streamToFile("contents".byteInputStream(), file.path)
            assertThat(file.readText()).isEqualTo("contents")
        } finally {
            file.delete()
        }
    }

    @Test
    fun `streamToFile creates parent directories and replaces existing content`(@TempDir directory: Path) {
        val file = directory.resolve("nested/upload.txt").toFile()
        FileUtil.streamToFile("original contents".byteInputStream(), file.path)
        FileUtil.streamToFile("new".byteInputStream(), file.path)
        assertThat(file.readText()).isEqualTo("new")
    }
}
