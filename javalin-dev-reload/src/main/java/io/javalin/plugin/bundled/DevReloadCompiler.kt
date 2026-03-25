package io.javalin.plugin.bundled

import io.javalin.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Handles compilation for [DevReloadPlugin].
 *
 * Two compilation strategies, tried in order:
 * 1. **Direct compiler** — invokes `javac`/`kotlinc` directly on only the changed files.
 *    For Kotlin, uses **in-process** compilation via the K2JVMCompiler API with a pre-warmed
 *    classloader (~250ms per compile). For Java, invokes `javac` as a subprocess (~0.8s).
 * 2. **Build tool fallback** — shells out to Maven, Gradle, or Bazel.
 *    Used when direct compilation fails or when `compileCommand` is set explicitly.
 *
 * Auto-detects Maven, Gradle, and Bazel when no explicit command is provided.
 */
internal class DevReloadCompiler(private val logExtensive: (String) -> Unit) {

    data class Result(val elapsedMs: Long, val success: Boolean, val output: String = "")

    /** Cached classloader for in-process Kotlin compilation. Kept warm across reloads. */
    @Volatile private var kotlinCompilerClassLoader: URLClassLoader? = null

    /**
     * Pre-loads the Kotlin compiler classloader in the background so the first compile is fast.
     * Call this at startup while the child process is launching.
     */
    fun warmup(classpath: String) {
        val compilerJar = findKotlinCompilerJar(classpath) ?: return
        Thread({
            try {
                logExtensive("DevReloadPlugin: Warming up Kotlin compiler...")
                val cl = URLClassLoader(arrayOf(compilerJar.toUri().toURL()), ClassLoader.getSystemClassLoader())
                // Force-load the compiler class to warm up the classloader
                cl.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
                kotlinCompilerClassLoader = cl
                logExtensive("DevReloadPlugin: Kotlin compiler ready")
            } catch (_: Exception) {}
        }, "DevReloadPlugin-warmup").apply { isDaemon = true; start() }
    }

    // --- Direct compiler (fast path) ---

    /**
     * Compiles only the changed source files directly.
     * - Kotlin: in-process via K2JVMCompiler (pre-warmed classloader = ~250ms)
     * - Java: subprocess via javac (~0.8s)
     * Returns null if the required compiler is not available.
     */
    fun compileDirect(changedSources: List<Path>, classpath: String, outputDir: Path): Result? {
        val javaFiles = changedSources.filter { it.toString().endsWith(".java") }
        val kotlinFiles = changedSources.filter { it.toString().endsWith(".kt") }
        if (javaFiles.isEmpty() && kotlinFiles.isEmpty()) return null

        val javacBin = if (javaFiles.isNotEmpty()) findJavac() ?: return null else null
        if (kotlinFiles.isNotEmpty() && findKotlinCompilerJar(classpath) == null) return null

        val jvmTarget = System.getProperty("java.specification.version", "17")
        val startTime = System.currentTimeMillis()
        val errors = StringBuilder()

        // Kotlin first (Java classes may depend on Kotlin, and Kotlin generates .class files javac can see)
        if (kotlinFiles.isNotEmpty()) {
            logExtensive("DevReloadPlugin: In-process kotlinc: ${kotlinFiles.map { it.fileName }}")
            val result = compileKotlinInProcess(kotlinFiles, classpath, outputDir, jvmTarget)
            if (!result.success) {
                errors.append(result.output)
                return Result(System.currentTimeMillis() - startTime, false, errors.toString())
            }
        }

        // Then Java (subprocess — javac is already fast)
        if (javaFiles.isNotEmpty()) {
            val args = mutableListOf(javacBin!!)
            args.addAll(listOf("-cp", classpath, "-d", outputDir.toString(), "-source", jvmTarget, "-target", jvmTarget, "-nowarn"))
            args.addAll(javaFiles.map { it.toString() })
            logExtensive("DevReloadPlugin: Direct javac: ${javaFiles.map { it.fileName }}")
            val result = runProcess(ProcessBuilder(args).directory(Path.of("").toAbsolutePath().toFile()).redirectErrorStream(true))
            if (!result.success) {
                errors.append(result.output)
                return Result(System.currentTimeMillis() - startTime, false, errors.toString())
            }
        }

        return Result(System.currentTimeMillis() - startTime, true)
    }

    /**
     * Compiles Kotlin files in-process using the K2JVMCompiler API.
     * Keeps the compiler classloader warm across invocations for fast recompilation.
     */
    private fun compileKotlinInProcess(kotlinFiles: List<Path>, classpath: String, outputDir: Path, jvmTarget: String): Result {
        val startTime = System.currentTimeMillis()
        try {
            val compilerJar = findKotlinCompilerJar(classpath)!!
            val cl = kotlinCompilerClassLoader ?: run {
                logExtensive("DevReloadPlugin: Loading Kotlin compiler from $compilerJar")
                URLClassLoader(arrayOf(compilerJar.toUri().toURL()), ClassLoader.getSystemClassLoader()).also {
                    kotlinCompilerClassLoader = it
                }
            }

            val compilerClass = cl.loadClass("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
            val compiler = compilerClass.getDeclaredConstructor().newInstance()
            val execMethod = compilerClass.getMethod("exec", PrintStream::class.java, Array<String>::class.java)

            val compilerArgs = mutableListOf("-cp", classpath, "-d", outputDir.toString(), "-jvm-target", jvmTarget, "-nowarn")
            compilerArgs.addAll(kotlinFiles.map { it.toString() })

            val outputCapture = ByteArrayOutputStream()
            val printStream = PrintStream(outputCapture)
            val result = execMethod.invoke(compiler, printStream, compilerArgs.toTypedArray())
            printStream.flush()
            val output = outputCapture.toString()

            val elapsed = System.currentTimeMillis() - startTime
            val resultName = result.toString() // "OK", "COMPILATION_ERROR", "INTERNAL_ERROR", etc.
            if (resultName == "OK") {
                return Result(elapsed, true)
            } else {
                JavalinLogger.warn("DevReloadPlugin: In-process kotlinc failed ($resultName, ${elapsed}ms)")
                if (output.isNotBlank()) output.lines().take(10).forEach { JavalinLogger.warn("  $it") }
                return Result(elapsed, false, output)
            }
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            JavalinLogger.warn("DevReloadPlugin: In-process kotlinc error (${elapsed}ms): ${e.message}")
            return Result(elapsed, false, e.message ?: "Unknown error")
        }
    }

    // --- Build tool (fallback) ---

    /** Compiles via build tool. Returns a result with elapsed time, success flag, and output on failure. */
    fun compile(compileCommand: String?): Result {
        val command = compileCommand ?: detectBuildCommand()
        if (command == null) {
            val msg = "No build tool found (no pom.xml, build.gradle, or Bazel workspace). " +
                "Set compileCommand (e.g., \"mvn compile -o -q\" or \"gradle classes -q\")."
            JavalinLogger.warn("DevReloadPlugin: $msg")
            return Result(-1, false, msg)
        }
        logExtensive("DevReloadPlugin: Running compile command: $command")
        val isWindows = System.getProperty("os.name", "").lowercase().contains("win")
        val shell = if (isWindows) listOf("cmd", "/c", command) else listOf("sh", "-c", command)
        return runProcess(ProcessBuilder(shell).directory(Path.of("").toAbsolutePath().toFile()).redirectErrorStream(true))
    }

    // --- Build tool detection ---

    /** Auto-detect build tool from the working directory. */
    internal fun detectBuildCommand(): String? {
        val cwd = Path.of("").toAbsolutePath()
        return when {
            Files.exists(cwd.resolve("pom.xml")) -> detectMavenCommand(cwd)
            Files.exists(cwd.resolve("build.gradle.kts")) || Files.exists(cwd.resolve("build.gradle")) -> detectGradleCommand(cwd)
            Files.exists(cwd.resolve("MODULE.bazel")) || Files.exists(cwd.resolve("WORKSPACE")) || Files.exists(cwd.resolve("WORKSPACE.bazel")) -> detectBazelCommand()
            else -> null
        }
    }

    /** Build the fastest Maven compile command for the project. */
    private fun detectMavenCommand(cwd: Path): String {
        val mvn = if (isMvndAvailable()) "mvnd" else "mvn"
        val pom = try { Files.readString(cwd.resolve("pom.xml")) } catch (_: Exception) { "" }
        val hasKotlin = pom.contains("kotlin-maven-plugin")
        val goals = if (hasKotlin) "kotlin:compile compiler:compile" else "compiler:compile"
        return "$mvn $goals -o -q"
    }

    /** Build the fastest Gradle compile command for the project. */
    private fun detectGradleCommand(cwd: Path): String {
        val gradle = when {
            Files.isExecutable(cwd.resolve("gradlew")) -> "./gradlew"
            Files.isExecutable(cwd.resolve("gradlew.bat")) &&
                System.getProperty("os.name", "").lowercase().contains("win") -> "gradlew.bat"
            else -> "gradle"
        }
        val buildFile = when {
            Files.exists(cwd.resolve("build.gradle.kts")) ->
                try { Files.readString(cwd.resolve("build.gradle.kts")) } catch (_: Exception) { "" }
            Files.exists(cwd.resolve("build.gradle")) ->
                try { Files.readString(cwd.resolve("build.gradle")) } catch (_: Exception) { "" }
            else -> ""
        }
        val hasKotlin = buildFile.contains("kotlin(") || buildFile.contains("org.jetbrains.kotlin")
        val tasks = if (hasKotlin) "compileKotlin compileJava" else "compileJava"
        return "$gradle $tasks --build-cache -q"
    }

    /** Build a Bazel compile command. */
    private fun detectBazelCommand(): String = "bazel build //... --noshow_progress"

    private fun isMvndAvailable(): Boolean = try {
        ProcessBuilder("mvnd", "--version").start().waitFor(5, TimeUnit.SECONDS)
    } catch (_: Exception) { false }

    // --- Compiler detection ---

    /** Find javac from java.home. */
    internal fun findJavac(): String? {
        val javaHome = System.getProperty("java.home", "").ifBlank { return null }
        val javac = Path.of(javaHome, "bin", "javac")
        val javacExe = Path.of(javaHome, "bin", "javac.exe")
        return when {
            Files.isExecutable(javac) -> javac.toString()
            Files.isExecutable(javacExe) -> javacExe.toString()
            else -> null
        }
    }

    /**
     * Find kotlin-compiler.jar for in-process compilation.
     * Checks IntelliJ bundled location and standard Kotlin installations.
     */
    internal fun findKotlinCompilerJar(classpath: String): Path? {
        // 1. Check IntelliJ bundled (when launched from IntelliJ)
        try {
            val cmdArgs = ProcessHandle.current().info().arguments().orElse(emptyArray())
            for (arg in cmdArgs) {
                if (arg.startsWith("-javaagent:") && arg.contains("idea_rt")) {
                    val agentPath = arg.removePrefix("-javaagent:").substringBefore("=")
                    val ideContents = Path.of(agentPath).parent?.parent
                    val compilerJar = ideContents?.resolve("plugins/Kotlin/kotlinc/lib/kotlin-compiler.jar")
                    if (compilerJar != null && Files.isRegularFile(compilerJar)) return compilerJar
                }
            }
        } catch (_: Exception) {}

        // 2. Derive from kotlin-stdlib on classpath → sibling kotlin-compiler.jar
        val pathSep = System.getProperty("path.separator", ":")
        val kotlinJar = classpath.split(pathSep)
            .firstOrNull { it.contains("kotlin-stdlib") && it.endsWith(".jar") && !it.contains("kotlin-stdlib-jdk") }
        if (kotlinJar != null) {
            val libDir = Path.of(kotlinJar).parent
            // Standard installation: lib/kotlin-stdlib.jar alongside lib/kotlin-compiler.jar
            val compilerJar = libDir?.resolve("kotlin-compiler.jar")
            if (compilerJar != null && Files.isRegularFile(compilerJar)) return compilerJar
        }

        // 3. Check common installation paths
        val commonPaths = listOf(
            "/usr/local/lib/kotlin/lib/kotlin-compiler.jar",
            "/usr/lib/kotlin/lib/kotlin-compiler.jar",
            System.getProperty("user.home", "") + "/.sdkman/candidates/kotlin/current/lib/kotlin-compiler.jar"
        )
        for (path in commonPaths) {
            val p = Path.of(path)
            if (Files.isRegularFile(p)) return p
        }

        return null
    }

    // --- Process execution ---

    /** Runs a process with output draining to avoid pipe buffer deadlock. */
    private fun runProcess(builder: ProcessBuilder): Result {
        val startTime = System.currentTimeMillis()
        try {
            val process = builder.start()
            var output = ""
            val drainer = Thread { output = process.inputStream.bufferedReader().readText() }
                .apply { isDaemon = true; start() }
            val finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                drainer.join(1000)
                val msg = "Compile timed out after ${PROCESS_TIMEOUT_SECONDS}s"
                JavalinLogger.warn("DevReloadPlugin: $msg")
                return Result(-1, false, msg)
            }
            drainer.join(5000)
            val elapsed = System.currentTimeMillis() - startTime
            if (process.exitValue() != 0) {
                JavalinLogger.warn("DevReloadPlugin: Compile failed (exit=${process.exitValue()}, ${elapsed}ms)")
                if (output.isNotBlank()) output.lines().take(10).forEach { JavalinLogger.warn("  $it") }
                return Result(elapsed, false, output)
            }
            return Result(elapsed, true)
        } catch (e: Exception) {
            val elapsed = System.currentTimeMillis() - startTime
            JavalinLogger.warn("DevReloadPlugin: Compile error (${elapsed}ms): ${e.message}")
            return Result(elapsed, false, e.message ?: "Unknown error")
        }
    }

    companion object {
        private const val PROCESS_TIMEOUT_SECONDS = 120L
    }
}
