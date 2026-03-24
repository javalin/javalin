package io.javalin.plugin.bundled

import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin
import io.javalin.util.JavalinLogger
import jakarta.servlet.DispatcherType
import java.net.ServerSocket
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import org.eclipse.jetty.ee10.servlet.FilterHolder

/**
 * **Experimental** — Development-mode plugin that reloads your app when source files change.
 *
 * Uses a process-restart approach: the plugin runs your app as a child process behind a
 * reverse proxy. When files change, it recompiles (via your build tool) and restarts the
 * child process. The proxy maintains a stable port so the browser sees a seamless reload.
 *
 * ```java
 * Javalin.create(config -> {
 *     config.routes.get("/hello", ctx -> ctx.result("Hello!"));
 *     config.registerPlugin(new DevReloadPlugin());
 * }).start(7070);
 * ```
 *
 * The plugin auto-detects Maven/Gradle/Bazel and the main class. For custom setups:
 * ```java
 * config.registerPlugin(new DevReloadPlugin(c -> {
 *     c.compileCommand = "mvn compile -o -q";
 *     c.mainClass = "com.myapp.MainKt";
 * }));
 * ```
 */
@Deprecated("Experimental — API may change in future releases.", level = DeprecationLevel.WARNING)
class DevReloadPlugin(userConfig: Consumer<Config>? = null) : Plugin<DevReloadPlugin.Config>(userConfig, Config()) {

    enum class LogLevel { NONE, BASIC, EXTENSIVE }

    class Config {
        /** Directories to watch for source file changes (.java, .kt). Auto-detected if empty. */
        @JvmField var sourceWatchPaths: List<Path> = emptyList()
        /** Directories to watch for compiled class/resource file changes. Auto-detected if empty. */
        @JvmField var classWatchPaths: List<Path> = emptyList()
        /** Custom shell command for compilation (e.g., "mvn compile -o -q"). Auto-detected if null. */
        @JvmField var compileCommand: String? = null
        /** The main class to launch as a child process. Auto-detected from sun.java.command if null. */
        @JvmField var mainClass: String? = null
        /** Log level: NONE, BASIC (default), or EXTENSIVE. */
        @JvmField var logging: LogLevel = LogLevel.BASIC
        /** Minimum ms between filesystem walks. Default 500ms. */
        @JvmField var watchCooldownMs: Long = 500
        /** Use direct javac/kotlinc invocation for fast single-file recompilation. Falls back to build tool on failure. */
        @JvmField var useDirectCompiler: Boolean = true
    }

    private fun logBasic(msg: String) { if (pluginConfig.logging >= LogLevel.BASIC) JavalinLogger.info(msg) }
    private fun logExtensive(msg: String) { if (pluginConfig.logging >= LogLevel.EXTENSIVE) JavalinLogger.info(msg) }

    override fun onStart(state: JavalinState) {
        if (isChildProcess()) {
            onStartChild(state)
        } else {
            onStartParent(state)
        }
    }

    // --- Child mode ---

    private fun onStartChild(state: JavalinState) {
        // Suppress noisy startup output in child — the parent handles user-facing logging
        state.startup.showJavalinBanner = false
        state.startup.showOldJavalinVersionWarning = false
        state.startup.startupWatcherEnabled = false

        val assignedPort = System.getProperty(CHILD_PORT_PROP)?.toIntOrNull()
        if (assignedPort != null && assignedPort > 0) {
            // Override the port via a server consumer, which runs at JettyServer.start() time
            // AFTER .start(port) has set state.jetty.port. This ensures we win over user config.
            state.jettyInternal.serverConsumers.add(0, java.util.function.Consumer {
                state.jetty.port = assignedPort
            })
        }
        // In child mode, the plugin does nothing else — the app runs normally.
    }

    // --- Parent mode ---

    private fun onStartParent(state: JavalinState) {
        // Replace the default Javalin banner with our dev-mode banner
        state.startup.showJavalinBanner = false
        state.startup.showOldJavalinVersionWarning = false

        // Resolve config early (during create()) so we can fail fast
        val mainClass = pluginConfig.mainClass ?: detectMainClass()
        if (mainClass == null) {
            JavalinLogger.warn(
                "DevReloadPlugin: Could not detect main class. " +
                "Set config.mainClass (e.g., \"com.myapp.MainKt\")."
            )
            return
        }
        val classpath = System.getProperty("java.class.path", "")
        val javaHome = System.getProperty("java.home", "")
        val javaBin = Path.of(javaHome, "bin", "java").toString()

        val classDirs = pluginConfig.classWatchPaths.ifEmpty { detectClasspathDirectories() }.resolveExisting()
        val sourceDirs = pluginConfig.sourceWatchPaths.ifEmpty { detectSourceDirectories() }.resolveExisting()
        val allWatchPaths = (sourceDirs + classDirs).distinct()
        if (allWatchPaths.isEmpty()) {
            JavalinLogger.warn("DevReloadPlugin: No directories to watch. Make sure your project is compiled.")
            return
        }

        logExtensive("DevReloadPlugin [parent]: Main class: $mainClass")
        logExtensive("DevReloadPlugin [parent]: Source paths: $sourceDirs")
        logExtensive("DevReloadPlugin [parent]: Class paths: $classDirs")
        logExtensive("DevReloadPlugin [parent]: Java: $javaBin")

        // Set up the proxy filter — intercepts ALL requests and forwards to child
        val proxy = DevReloadProxy()
        state.jettyInternal.servletContextHandlerConsumers.add { handler ->
            handler.addFilter(FilterHolder(proxy), "/*", EnumSet.of(DispatcherType.REQUEST))
        }

        val compiler = DevReloadCompiler(::logExtensive)
        compiler.warmup(classpath) // pre-load Kotlin compiler in background while child launches
        val jvmArgs = collectJvmArgs()

        // Defer child launch to JettyServer.start() time, when state.jetty.port
        // reflects the actual port from .start(port) rather than the default 8080.
        state.jettyInternal.serverConsumers.add(java.util.function.Consumer {
            val actualPort = state.jetty.port
            val watcher = DevReloadWatcher(allWatchPaths, pluginConfig.watchCooldownMs)
            val sourcePathSet = sourceDirs.toSet()
            val reloading = AtomicBoolean(false)

            // Launch the first child process
            var childProcess = launchChild(javaBin, classpath, jvmArgs, mainClass, actualPort)
            if (childProcess != null) {
                proxy.targetPort = childProcess.port
            } else {
                JavalinLogger.warn("DevReloadPlugin: Failed to start child process.")
            }

            // Print banner AFTER all Jetty/Javalin startup logging has finished
            state.events.serverStarted { JavalinLogger.startup(devReloadBanner(actualPort)) }

            // On-demand reload: called by the proxy on each incoming request.
            // Checks for changes, then kicks off compile+restart in the background.
            // All requests (including the triggering one) get the spinner page immediately;
            // the JS polls /__dev-reload/status and reloads when the new child is ready.
            proxy.reloadCheck = check@{
                // If a reload is already in progress, skip entirely
                if (reloading.get()) return@check

                val changed = watcher.checkForChanges()
                if (changed.isEmpty()) return@check

                val sourceChanges = changed.filter { file ->
                    (file.toString().endsWith(".java") || file.toString().endsWith(".kt"))
                        && sourcePathSet.any { file.startsWith(it) }
                }
                val classChanges = changed.filter { file ->
                    (file.toString().endsWith(".class") || file.toString().endsWith(".properties"))
                        && sourcePathSet.none { file.startsWith(it) }
                }
                if (sourceChanges.isEmpty() && classChanges.isEmpty()) return@check

                // CAS ensures only one thread wins
                if (!reloading.compareAndSet(false, true)) return@check

                val names = (sourceChanges + classChanges).map { it.fileName.toString() }
                logBasic("DevReloadPlugin: Changes detected in $names")

                // Set spinner state so ALL requests (including this one) get the spinner
                proxy.compileError = null
                proxy.reloadingFiles = names

                // Compile + restart in background thread so this request returns immediately
                Thread({
                    try {
                        if (sourceChanges.isNotEmpty()) {
                            val result = compileChanged(compiler, sourceChanges, classpath, classDirs)
                            if (!result.success) {
                                proxy.compileError = result.output
                                killChild(childProcess)
                                childProcess = null
                                proxy.targetPort = -1
                                return@Thread
                            }
                            logBasic("DevReloadPlugin: Compiled in ${result.elapsedMs}ms")
                        }

                        val oldChild = childProcess
                        killChild(oldChild)
                        watcher.resetBaseline()
                        val newChild = launchChild(javaBin, classpath, jvmArgs, mainClass, actualPort)
                        childProcess = newChild
                        if (newChild != null) {
                            proxy.targetPort = newChild.port
                            proxy.reloadingFiles = emptyList()
                            logBasic("DevReloadPlugin: Restarted child on port ${newChild.port}")
                        } else {
                            proxy.targetPort = -1
                            JavalinLogger.warn("DevReloadPlugin: Failed to restart child process.")
                        }
                    } catch (e: Exception) {
                        JavalinLogger.warn("DevReloadPlugin: Reload error — ${e.message}")
                    } finally {
                        reloading.set(false)
                    }
                }, "DevReloadPlugin-reload").start()
            }
        })
    }

    private data class ChildProcess(val process: Process, val port: Int, val outputThread: Thread)

    private fun launchChild(javaBin: String, classpath: String, jvmArgs: List<String>, mainClass: String, parentPort: Int): ChildProcess? {
        try {
            val childPort = findFreePort()
            val command = mutableListOf(javaBin)
            command.addAll(jvmArgs)
            command.add("-D$CHILD_FLAG_PROP=true")
            command.add("-D$CHILD_PORT_PROP=$childPort")
            command.add("-cp")
            command.add(classpath)
            command.add(mainClass)

            // Forward the original args (from sun.java.command, after the main class name)
            val sunCmd = System.getProperty("sun.java.command", "")
            val parts = sunCmd.split(" ")
            if (parts.size > 1) command.addAll(parts.drop(1))

            logExtensive("DevReloadPlugin [parent]: Launching child: ${command.joinToString(" ") { if (it.contains(" ")) "\"$it\"" else it }}")

            val process = ProcessBuilder(command)
                .directory(Path.of("").toAbsolutePath().toFile())
                .redirectErrorStream(true)
                .start()

            // Drain child output to parent's stdout, and watch for "Listening on" to confirm startup
            val ready = java.util.concurrent.CountDownLatch(1)
            val outputThread = Thread({
                try {
                    process.inputStream.bufferedReader().use { reader ->
                        reader.forEachLine { line ->
                            if (line.contains("Listening on") || line.contains("Javalin started")) {
                                ready.countDown()
                            } else if (isChildLineWorthShowing(line)) {
                                println("[dev-reload] $line")
                            }
                        }
                    }
                } catch (_: Exception) {}
                ready.countDown() // unblock await if process exited without startup message
            }, "DevReloadPlugin-child-output")
            outputThread.isDaemon = true
            outputThread.start()

            // Wait for child to be ready (or timeout)
            val started = ready.await(30, TimeUnit.SECONDS)
            if (!started) {
                if (process.isAlive) {
                    JavalinLogger.warn("DevReloadPlugin: Child process didn't print startup message within 30s, proceeding anyway")
                } else {
                    JavalinLogger.warn("DevReloadPlugin: Child process exited with code ${process.exitValue()}")
                    return null
                }
            }

            return ChildProcess(process, childPort, outputThread)
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: Failed to launch child — ${e.message}")
            return null
        }
    }

    private fun killChild(child: ChildProcess?) {
        if (child == null) return
        try {
            child.process.destroy()
            if (!child.process.waitFor(5, TimeUnit.SECONDS)) {
                child.process.destroyForcibly()
            }
        } catch (_: Exception) {}
    }

    /**
     * Compile changed source files. Tries direct javac/kotlinc first (fast path),
     * then falls back to the build tool command.
     */
    private fun compileChanged(
        compiler: DevReloadCompiler,
        sourceChanges: List<Path>,
        classpath: String,
        classDirs: List<Path>
    ): DevReloadCompiler.Result {
        // Try direct compilation first (skip if user set an explicit compile command)
        if (pluginConfig.useDirectCompiler && pluginConfig.compileCommand == null && classDirs.isNotEmpty()) {
            val directResult = compiler.compileDirect(sourceChanges, classpath, classDirs.first())
            if (directResult != null) {
                if (directResult.success) {
                    logBasic("DevReloadPlugin: Direct compile succeeded (${directResult.elapsedMs}ms)")
                    return directResult
                }
                // Direct compile failed — fall back to build tool
                logBasic("DevReloadPlugin: Direct compile failed, falling back to build tool")
            } else {
                logBasic("DevReloadPlugin: Direct compiler not available, using build tool")
            }
        }
        return compiler.compile(pluginConfig.compileCommand)
    }

    /** Collect JVM arguments (-D, -X, -ea, etc.) from the current process, excluding our own markers. */
    private fun collectJvmArgs(): List<String> {
        val args = mutableListOf<String>()
        // Try ProcessHandle first (Java 9+)
        try {
            val info = ProcessHandle.current().info()
            val cmdArgs = info.arguments().orElse(emptyArray())
            var skip = false
            for (arg in cmdArgs) {
                if (skip) { skip = false; continue }
                // Skip the classpath (we pass it explicitly) and the main class + everything after
                if (arg == "-cp" || arg == "-classpath") { skip = true; continue }
                if (!arg.startsWith("-")) break // reached main class
                if (arg.startsWith("-D$CHILD_FLAG_PROP") || arg.startsWith("-D$CHILD_PORT_PROP")) continue
                if (arg.contains("idea_rt.jar")) continue // IntelliJ agent — skip for child
                if (arg.startsWith("-javaagent:") && arg.contains("idea_rt")) continue
                args.add(arg)
            }
        } catch (_: Exception) {
            // Fallback: just forward common system properties
            for (prop in listOf("file.encoding", "sun.stdout.encoding", "sun.stderr.encoding")) {
                System.getProperty(prop)?.let { args.add("-D$prop=$it") }
            }
        }
        return args
    }

    companion object {
        private const val CHILD_FLAG_PROP = "javalin.devreload.child"
        private const val CHILD_PORT_PROP = "javalin.devreload.port"

        /** Jetty/Javalin startup noise patterns to suppress from child output. */
        private val CHILD_NOISE_PATTERNS = listOf(
            "jetty-12.", "oejs.", "oeje10s.", "SESSIONS", "SessionHandler",
            "DefaultSessionIdManager", "ServletContextHandler", "ServerConnector",
            "ContextHandler", "Starting Javalin", "Javalin started",
            "javalin.io/documentation", "You are running Javalin",
            "____/", "__  /", "/ /_/", "/ /__ ", "\\____", "__,_/",  // banner art
            "https://javalin.io", "Listening on",
        )

        /** Returns true if a child output line is worth showing to the user. */
        private fun isChildLineWorthShowing(line: String): Boolean {
            if (line.isBlank()) return false
            if (line.contains("ERROR") || line.contains("WARN") || line.contains("Exception")) return true
            return CHILD_NOISE_PATTERNS.none { line.contains(it) }
        }

        internal fun isChildProcess(): Boolean = System.getProperty(CHILD_FLAG_PROP) == "true"

        private fun findFreePort(): Int = ServerSocket(0).use { it.localPort }

        private fun detectMainClass(): String? {
            val sunCmd = System.getProperty("sun.java.command", "")
            val mainClass = sunCmd.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }
            // sun.java.command might contain a jar path instead of a class name
            if (mainClass != null && !mainClass.endsWith(".jar")) return mainClass
            return null
        }

        internal fun detectClasspathDirectories(): List<Path> {
            val sep = FileSystems.getDefault().separator
            return System.getProperty("java.class.path", "")
                .split(System.getProperty("path.separator", ":"))
                .map { Path.of(it) }
                .filter { Files.isDirectory(it) }
                .filter { s ->
                    val p = s.toString()
                    p.contains("target${sep}classes") || p.contains("target${sep}test-classes") ||  // Maven
                        p.contains("build${sep}classes") ||                                          // Gradle
                        p.contains("bazel-bin${sep}") || p.contains("bazel-out${sep}")               // Bazel
                }
                .distinct()
        }

        internal fun detectSourceDirectories(): List<Path> {
            val sourceDirs = mutableListOf<Path>()
            for (classDir in detectClasspathDirectories()) {
                val p = classDir.toString()
                val moduleRoot = when {
                    p.contains("target") -> classDir.parent?.parent // Maven: target/classes → module root
                    p.contains("build") -> classDir.let { var d = it; while (d.parent != null && d.fileName.toString() != "build") d = d.parent; d.parent } // Gradle: build/classes/... → module root
                    p.contains("bazel-bin") || p.contains("bazel-out") -> {
                        // Bazel: bazel-bin is a symlink at workspace root, so workspace root is cwd
                        Path.of("").toAbsolutePath()
                    }
                    else -> null
                }
                if (moduleRoot != null) {
                    listOf("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin")
                        .map { moduleRoot.resolve(it) }.filter { Files.isDirectory(it) }
                        .forEach { sourceDirs.add(it.toAbsolutePath().normalize()) }
                }
            }
            return sourceDirs.distinct()
        }

        private fun List<Path>.resolveExisting() = map { it.toAbsolutePath().normalize() }.filter { Files.isDirectory(it) }

        private fun devReloadBanner(port: Int) = """
            |
            |       __                  ___          _____
            |      / /___ __   ______ _/ (_)___     /__  /
            | __  / / __ `/ | / / __ `/ / / __ \      / /
            |/ /_/ / /_/ /| |/ / /_/ / / / / / /     / /
            |\____/\__,_/ |___/\__,_/_/_/_/ /_/     /_/
            |
            |  ⚡ DEV RELOAD — http://localhost:$port/
            |  Editing source files will trigger recompile + restart
            |""".trimMargin()
    }
}
