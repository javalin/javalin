/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.FileUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIOException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

class TestFileUtil {

    @Test
    fun `streamToFile closes input when destination creation fails`(@TempDir directory: Path) {
        val source = directory.resolve("source.txt").toFile().apply { writeText("contents") }
        val blockedParent = directory.resolve("not-a-directory").toFile().apply { writeText("blocked") }
        val input = source.inputStream()
        try {
            assertThatIOException().isThrownBy {
                FileUtil.streamToFile(input, File(blockedParent, "upload.txt").path)
            }
            assertThat(input.channel.isOpen).isFalse()
        } finally {
            input.close()
        }
    }

    @Test
    fun `streamToFile closes input after successful copy`(@TempDir directory: Path) {
        val source = directory.resolve("source.txt").toFile().apply { writeText("contents") }
        val target = directory.resolve("target.txt").toFile()
        val input = source.inputStream()
        try {
            FileUtil.streamToFile(input, target.path)
            assertThat(target.readText()).isEqualTo("contents")
            assertThat(input.channel.isOpen).isFalse()
        } finally {
            input.close()
        }
    }

    @Test
    fun `streamToFile closes input when copy fails`(@TempDir directory: Path) {
        val source = directory.resolve("source.txt").toFile().apply { writeText("contents") }
        val target = directory.resolve("non-empty-directory").toFile().apply { mkdir() }
        File(target, "child.txt").writeText("keep")
        val input = source.inputStream()
        try {
            assertThatIOException().isThrownBy {
                FileUtil.streamToFile(input, target.path)
            }
            assertThat(input.channel.isOpen).isFalse()
        } finally {
            input.close()
        }
    }

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
