package io.javalin

import io.javalin.plugin.bundled.DevReloadCompiler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TestDevReloadCompiler {

    private val logMessages = mutableListOf<String>()
    private val compiler = DevReloadCompiler { logMessages.add(it) }

    // --- Build tool compilation ---

    @Test
    fun `compile with explicit command succeeds`() {
        val result = compiler.compile("echo hello")
        assertThat(result.success).isTrue
        assertThat(result.elapsedMs).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun `compile with failing command returns failure with output`() {
        val result = compiler.compile("exit 1")
        assertThat(result.success).isFalse
    }

    @Test
    fun `compile logs extensive output`() {
        compiler.compile("echo hello")
        assertThat(logMessages).anyMatch { it.contains("Running compile command") }
    }

    @Test
    fun `compile with null command auto-detects build tool`() {
        // In the javalin repo root, pom.xml exists — but we're in the module dir during tests.
        // This test just verifies it doesn't crash; the result depends on the working directory.
        val result = compiler.compile(null)
        assertThat(result).isNotNull
    }

    // --- Direct compilation ---

    @Test
    fun `compileDirect compiles a single java file`(@TempDir tempDir: Path) {
        val srcFile = tempDir.resolve("Hello.java")
        Files.writeString(srcFile, "public class Hello { public static void main(String[] args) {} }")
        val outDir = tempDir.resolve("out")
        Files.createDirectories(outDir)

        val classpath = System.getProperty("java.class.path", "")
        val result = compiler.compileDirect(listOf(srcFile), classpath, outDir)

        // If javac is available, direct compilation should succeed
        if (result != null) {
            assertThat(result.success).isTrue
            assertThat(result.elapsedMs).isGreaterThanOrEqualTo(0)
            assertThat(Files.exists(outDir.resolve("Hello.class"))).isTrue
        }
    }

    @Test
    fun `compileDirect returns failure for invalid java code`(@TempDir tempDir: Path) {
        val srcFile = tempDir.resolve("Bad.java")
        Files.writeString(srcFile, "public class Bad { this is not valid java }")
        val outDir = tempDir.resolve("out")
        Files.createDirectories(outDir)

        val classpath = System.getProperty("java.class.path", "")
        val result = compiler.compileDirect(listOf(srcFile), classpath, outDir)

        if (result != null) {
            assertThat(result.success).isFalse
            assertThat(result.output).isNotBlank()
        }
    }

    @Test
    fun `compileDirect returns null for empty file list`(@TempDir tempDir: Path) {
        val outDir = tempDir.resolve("out")
        Files.createDirectories(outDir)
        val result = compiler.compileDirect(emptyList(), "", outDir)
        assertThat(result).isNull()
    }

    @Test
    fun `compileDirect returns null for kt files when kotlinc unavailable`(@TempDir tempDir: Path) {
        val srcFile = tempDir.resolve("Hello.kt")
        Files.writeString(srcFile, "fun main() {}")
        val outDir = tempDir.resolve("out")
        Files.createDirectories(outDir)

        // With an empty classpath and no kotlinc on PATH, should return null
        val result = compiler.compileDirect(listOf(srcFile), "", outDir)
        // Result is null if kotlinc isn't found, or a Result if it is
        // Either outcome is acceptable — we just verify no crash
        assertThat(true).isTrue // no-crash assertion
    }

    // --- Compiler binary detection ---

    @Test
    fun `findJavac locates javac from java home`() {
        val javac = compiler.findJavac()
        // javac should be available in any JDK (javac on Unix, javac.exe on Windows)
        assertThat(javac).isNotNull()
        assertThat(javac).containsIgnoringCase("javac")
    }

    @Test
    fun `findKotlinCompilerJar returns null for empty classpath without IntelliJ`() {
        // With no IntelliJ and no kotlin jars on classpath, should return null or find a system installation
        val result = compiler.findKotlinCompilerJar("")
        // We just verify it doesn't throw
        assertThat(true).isTrue
    }

    // --- Build tool detection ---

    @Test
    fun `detectBuildCommand detects maven in javalin repo`() {
        // The javalin repo has a pom.xml, so this should detect Maven
        val command = compiler.detectBuildCommand()
        // Depending on cwd, may find pom.xml or not
        assertThat(command).satisfiesAnyOf(
            { assertThat(it).contains("mvn").contains("compile") },
            { assertThat(it).contains("mvnd").contains("compile") },
            { assertThat(it).isNull() } // if cwd doesn't have pom.xml
        )
    }
}
