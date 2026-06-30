package io.javalin

import io.javalin.plugin.bundled.DevReloadWatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TestDevReloadWatcher {

    @Test
    fun `detects new file`(@TempDir dir: Path) {
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 0)
        // Initial check — no changes
        assertThat(watcher.checkForChanges()).isEmpty()

        // Create a new file
        Files.writeString(dir.resolve("Test.java"), "class Test {}")
        Thread.sleep(50) // ensure timestamp differs
        assertThat(watcher.checkForChanges()).isNotEmpty
    }

    @Test
    fun `detects modified file`(@TempDir dir: Path) {
        val file = dir.resolve("Test.java")
        Files.writeString(file, "class Test { int v1; }")
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 0)
        watcher.checkForChanges() // establish baseline

        Thread.sleep(50)
        Files.writeString(file, "class Test { int v2; }")
        assertThat(watcher.checkForChanges()).contains(file)
    }

    @Test
    fun `detects deleted file`(@TempDir dir: Path) {
        val file = dir.resolve("Test.java")
        Files.writeString(file, "class Test {}")
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 0)
        watcher.checkForChanges() // establish baseline

        Files.delete(file)
        val changes = watcher.checkForChanges()
        assertThat(changes).contains(file)
    }

    @Test
    fun `no changes when nothing changed`(@TempDir dir: Path) {
        Files.writeString(dir.resolve("Test.java"), "class Test {}")
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 0)
        watcher.checkForChanges() // establish baseline

        assertThat(watcher.checkForChanges()).isEmpty()
    }

    @Test
    fun `respects cooldown`(@TempDir dir: Path) {
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 5000)
        watcher.checkForChanges() // establish baseline

        Files.writeString(dir.resolve("Test.java"), "class Test {}")
        // Within cooldown — should return empty
        assertThat(watcher.checkForChanges()).isEmpty()
    }

    @Test
    fun `resetBaseline clears change detection`(@TempDir dir: Path) {
        val file = dir.resolve("Test.java")
        Files.writeString(file, "v1")
        val watcher = DevReloadWatcher(listOf(dir), cooldownMs = 0)
        watcher.checkForChanges() // baseline

        Thread.sleep(50)
        Files.writeString(file, "v2")
        watcher.resetBaseline() // snapshot new state
        assertThat(watcher.checkForChanges()).isEmpty()
    }

    @Test
    fun `watches multiple directories`(@TempDir dir1: Path, @TempDir dir2: Path) {
        val watcher = DevReloadWatcher(listOf(dir1, dir2), cooldownMs = 0)
        watcher.checkForChanges() // baseline

        val file1 = dir1.resolve("A.java")
        val file2 = dir2.resolve("B.java")
        Files.writeString(file1, "class A {}")
        Files.writeString(file2, "class B {}")
        Thread.sleep(50)

        val changes = watcher.checkForChanges()
        assertThat(changes).contains(file1, file2)
    }
}
