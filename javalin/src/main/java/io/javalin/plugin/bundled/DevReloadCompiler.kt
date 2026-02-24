package io.javalin.plugin.bundled

import io.javalin.util.JavalinLogger
import java.nio.file.Path

/**
 * Handles compilation of source files for [DevReloadPlugin].
 * Supports three strategies:
 * 1. Custom shell command ([compileViaCommand]) — delegates to the user's build tool
 * 2. In-process javax.tools compiler ([compileViaToolProvider]) — fast, no fork
 * 3. Forked javac process ([compileViaProcess]) — fallback when javax.tools is unavailable
 */
internal class DevReloadCompiler(
    private val classOutputPaths: List<Path>,
    private val logExtensive: (String) -> Unit
) {

    /**
     * Compiles the given source files. Returns the elapsed time in ms on success, or -1 on failure.
     */
    fun compile(sourceFiles: List<Path>, compileCommand: String?): Long {
        if (compileCommand != null) {
            return compileViaCommand(compileCommand)
        }

        val outputDir = classOutputPaths.firstOrNull()?.toAbsolutePath()?.normalize()?.toString()
        if (outputDir == null) {
            JavalinLogger.warn("DevReloadPlugin: No class output directory found for compilation")
            return -1
        }

        logExtensive("DevReloadPlugin: Compiling ${sourceFiles.map { it.fileName }} to $outputDir")
        val startTime = System.currentTimeMillis()

        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            JavalinLogger.warn("DevReloadPlugin: No in-process compiler available (running on JRE?), falling back to javac process")
            return compileViaProcess(sourceFiles, outputDir)
        }

        return compileViaToolProvider(compiler, sourceFiles, outputDir, startTime)
    }

    /** Compiles using the in-process javax.tools Java compiler. Returns elapsed ms or -1. */
    private fun compileViaToolProvider(
        compiler: javax.tools.JavaCompiler,
        sourceFiles: List<Path>,
        outputDir: String,
        startTime: Long
    ): Long {
        try {
            val classpath = System.getProperty("java.class.path", "")
            val options = listOf("-classpath", classpath, "-d", outputDir)
            val fileManager = compiler.getStandardFileManager(null, null, null)
            val compilationUnits = fileManager.getJavaFileObjects(*sourceFiles.map { it.toFile() }.toTypedArray())
            val diagnostics = javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>()
            val success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call()
            fileManager.close()

            val elapsed = System.currentTimeMillis() - startTime
            if (!success) {
                JavalinLogger.warn("DevReloadPlugin: Compile failed (${elapsed}ms)")
                diagnostics.diagnostics
                    .filter { it.kind == javax.tools.Diagnostic.Kind.ERROR }
                    .take(10)
                    .forEach { JavalinLogger.warn("  ${it.getMessage(null)}") }
                return -1
            }
            return elapsed
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: Compilation failed: ${e.message}")
            return -1
        }
    }

    /** Fallback: fork a javac process. Returns elapsed ms or -1. */
    private fun compileViaProcess(sourceFiles: List<Path>, outputDir: String): Long {
        val classpath = System.getProperty("java.class.path", "")
        val javac = Path.of(System.getProperty("java.home"), "bin", "javac").toString()
        val command = mutableListOf(javac, "-classpath", classpath, "-d", outputDir)
        sourceFiles.forEach { command.add(it.toAbsolutePath().toString()) }

        val startTime = System.currentTimeMillis()
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        val elapsed = System.currentTimeMillis() - startTime

        if (exitCode != 0) {
            JavalinLogger.warn("DevReloadPlugin: Compile failed (${elapsed}ms)")
            if (output.isNotBlank()) {
                output.lines().take(10).forEach { JavalinLogger.warn("  $it") }
            }
            return -1
        }
        return elapsed
    }

    /** Runs the user-configured compile command. Returns elapsed ms or -1. */
    private fun compileViaCommand(command: String): Long {
        logExtensive("DevReloadPlugin: Running compile command: $command")
        val startTime = System.currentTimeMillis()
        try {
            val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
            val shell = if (isWindows) listOf("cmd", "/c", command) else listOf("sh", "-c", command)
            val process = ProcessBuilder(shell)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            val elapsed = System.currentTimeMillis() - startTime

            if (exitCode != 0) {
                JavalinLogger.warn("DevReloadPlugin: Compile command failed (exit=$exitCode, ${elapsed}ms)")
                if (output.isNotBlank()) {
                    output.lines().take(10).forEach { JavalinLogger.warn("  $it") }
                }
                return -1
            }
            return elapsed
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            JavalinLogger.warn("DevReloadPlugin: Compile command error (${elapsed}ms): ${e.message}")
            return -1
        }
    }
}
