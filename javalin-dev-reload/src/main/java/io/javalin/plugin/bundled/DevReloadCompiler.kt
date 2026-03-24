package io.javalin.plugin.bundled

import io.javalin.util.JavalinLogger
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Handles compilation of source files for [DevReloadPlugin].
 * Supports: custom shell command, in-process javax.tools, or forked javac fallback.
 */
internal class DevReloadCompiler(
    private val classOutputPaths: List<Path>,
    private val logExtensive: (String) -> Unit
) {

    /** Compiles the given source files. Returns elapsed ms on success, or -1 on failure. */
    fun compile(sourceFiles: List<Path>, compileCommand: String?): Long {
        if (compileCommand != null) return compileViaCommand(compileCommand)
        val outputDir = classOutputPaths.firstOrNull()?.toAbsolutePath()?.normalize()?.toString()
            ?: return (-1L).also { JavalinLogger.warn("DevReloadPlugin: No class output directory found for compilation") }
        logExtensive("DevReloadPlugin: Compiling ${sourceFiles.map { it.fileName }} to $outputDir")
        val compiler = javax.tools.ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            JavalinLogger.warn("DevReloadPlugin: No in-process compiler (JRE?), falling back to javac process")
            return compileViaProcess(sourceFiles, outputDir)
        }
        return compileViaToolProvider(compiler, sourceFiles, outputDir)
    }

    private fun compileViaToolProvider(compiler: javax.tools.JavaCompiler, sourceFiles: List<Path>, outputDir: String): Long {
        try {
            val classpath = System.getProperty("java.class.path", "")
            val fileManager = compiler.getStandardFileManager(null, null, null)
            val units = fileManager.getJavaFileObjects(*sourceFiles.map { it.toFile() }.toTypedArray())
            val diagnostics = javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>()
            val startTime = System.currentTimeMillis()
            val success = compiler.getTask(null, fileManager, diagnostics, listOf("-classpath", classpath, "-d", outputDir), null, units).call()
            fileManager.close()
            val elapsed = System.currentTimeMillis() - startTime
            if (!success) {
                JavalinLogger.warn("DevReloadPlugin: Compile failed (${elapsed}ms)")
                diagnostics.diagnostics.filter { it.kind == javax.tools.Diagnostic.Kind.ERROR }.take(10)
                    .forEach { JavalinLogger.warn("  ${it.getMessage(null)}") }
                return -1
            }
            return elapsed
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: Compilation failed: ${e.message}")
            return -1
        }
    }

    private fun compileViaProcess(sourceFiles: List<Path>, outputDir: String): Long {
        val classpath = System.getProperty("java.class.path", "")
        val javac = Path.of(System.getProperty("java.home"), "bin", "javac").toString()
        val command = mutableListOf(javac, "-classpath", classpath, "-d", outputDir)
        sourceFiles.forEach { command.add(it.toAbsolutePath().toString()) }
        return runProcess(ProcessBuilder(command).redirectErrorStream(true), "javac")
    }

    private fun compileViaCommand(command: String): Long {
        logExtensive("DevReloadPlugin: Running compile command: $command")
        val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
        val shell = if (isWindows) listOf("cmd", "/c", command) else listOf("sh", "-c", command)
        return runProcess(ProcessBuilder(shell).directory(Path.of("").toAbsolutePath().toFile()).redirectErrorStream(true), "compile command")
    }

    /** Runs a process with output draining in a virtual thread to avoid pipe buffer deadlock. */
    private fun runProcess(builder: ProcessBuilder, label: String): Long {
        val startTime = System.currentTimeMillis()
        try {
            val process = builder.start()
            var output = ""
            val drainer = Thread.ofVirtual().start {
                output = process.inputStream.bufferedReader().readText()
            }
            val finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                drainer.join(1000)
                JavalinLogger.warn("DevReloadPlugin: $label timed out after ${PROCESS_TIMEOUT_SECONDS}s")
                return -1
            }
            drainer.join(5000)
            val elapsed = System.currentTimeMillis() - startTime
            if (process.exitValue() != 0) {
                JavalinLogger.warn("DevReloadPlugin: $label failed (exit=${process.exitValue()}, ${elapsed}ms)")
                if (output.isNotBlank()) output.lines().take(10).forEach { JavalinLogger.warn("  $it") }
                return -1
            }
            return elapsed
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: $label error (${System.currentTimeMillis() - startTime}ms): ${e.message}")
            return -1
        }
    }

    companion object {
        private const val PROCESS_TIMEOUT_SECONDS = 120L
    }
}
