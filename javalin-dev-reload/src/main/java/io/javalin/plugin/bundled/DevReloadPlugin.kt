package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin
import io.javalin.util.JavalinLogger
import io.javalin.validation.Validation.Companion.addValidationExceptionMapper
import jakarta.servlet.DispatcherType
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import java.util.function.Consumer
import org.eclipse.jetty.ee10.servlet.FilterHolder

/**
 * **Experimental** — Development-mode plugin that reloads routes when source or class files change.
 *
 * Checks for file changes on each incoming HTTP request. If changes are detected, it compiles
 * (if source files changed) and re-executes the original config consumer from [io.javalin.Javalin.create].
 * The Jetty server continues running on the same port — only the routing tables are swapped.
 *
 * **Important:** Construct *inside* the `Javalin.create()` config lambda so it can capture the
 * config consumer via StackWalker. If constructed outside, set [Config.onReload] explicitly.
 *
 * ```java
 * Javalin.create(config -> {
 *     config.routes.get("/hello", ctx -> ctx.result("Hello!"));
 *     config.registerPlugin(new DevReloadPlugin());
 * }).start(7070);
 * ```
 */
@Deprecated("Experimental — API may change in future releases.", level = DeprecationLevel.WARNING)
class DevReloadPlugin(userConfig: Consumer<Config>? = null) : Plugin<DevReloadPlugin.Config>(userConfig, Config()) {

    // Lambda method name + declaring class, captured at construction time via StackWalker
    private val originalLambdaInfo: Pair<String, String>? = StackWalker.getInstance().walk { frames ->
        frames.filter { it.methodName.contains("lambda") }
            .map { it.methodName to it.className }
            .findFirst().orElse(null)
    }

    private var previousClassLoader: URLClassLoader? = null

    enum class LogLevel { NONE, BASIC, EXTENSIVE }

    class Config {
        /** Directories to watch for source file changes (.java, .kt). Auto-detected if empty. */
        @JvmField var sourceWatchPaths: List<Path> = emptyList()
        /** Directories to watch for compiled class file changes (.class). Auto-detected if empty. */
        @JvmField var classWatchPaths: List<Path> = emptyList()
        /** Directories the fresh classloader uses to load updated classes. Defaults to classWatchPaths. */
        @JvmField var classOutputPaths: List<Path> = emptyList()
        /** Override for the config consumer to re-execute on reload. Auto-captured if null. */
        @JvmField var onReload: Consumer<JavalinConfig>? = null
        /** Custom shell command for compilation (e.g., "mvn compile -o -q"). Uses javax.tools if null. */
        @JvmField var compileCommand: String? = null
        /** Log level: NONE, BASIC (default), or EXTENSIVE. */
        @JvmField var logging: LogLevel = LogLevel.BASIC
        /** Minimum ms between filesystem walks. Default 500ms. */
        @JvmField var watchCooldownMs: Long = 500
    }

    private fun logBasic(msg: String) { if (pluginConfig.logging >= LogLevel.BASIC) JavalinLogger.info(msg) }
    private fun logExtensive(msg: String) { if (pluginConfig.logging >= LogLevel.EXTENSIVE) JavalinLogger.info(msg) }

    override fun onStart(state: JavalinState) {
        val reloadConsumer = pluginConfig.onReload ?: DevReloadReflection.captureUserConfig(originalLambdaInfo, ::logExtensive)
        if (reloadConsumer == null) {
            JavalinLogger.warn(
                "DevReloadPlugin: Could not auto-detect the Javalin.create() config consumer. " +
                "Construct the plugin inline, or set onReload explicitly."
            )
            return
        }

        val classOutputDirs = pluginConfig.classOutputPaths.ifEmpty { detectClasspathDirectories() }
        val classPaths = pluginConfig.classWatchPaths.ifEmpty { classOutputDirs }.resolveExisting()
        val sourcePaths = pluginConfig.sourceWatchPaths.ifEmpty { detectSourceDirectories() }.resolveExisting()
        val allWatchPaths = (sourcePaths + classPaths).distinct()
        if (allWatchPaths.isEmpty()) {
            JavalinLogger.warn("DevReloadPlugin: No directories to watch. Make sure your project is compiled.")
            return
        }

        logExtensive("DevReloadPlugin: Working directory: ${Path.of("").toAbsolutePath()}")
        logExtensive("DevReloadPlugin: Source paths: $sourcePaths")
        logExtensive("DevReloadPlugin: Class paths: $classPaths")

        val sourcePathSet = sourcePaths.toSet()
        val watcher = DevReloadWatcher(allWatchPaths, pluginConfig.watchCooldownMs)
        val compiler = DevReloadCompiler(classOutputDirs, ::logExtensive)

        JavalinLogger.startup("DevReloadPlugin: Watching for changes (on-request)")

        val reloadFilter = jakarta.servlet.Filter { req, res, chain ->
            checkAndReload(watcher, compiler, sourcePathSet, classOutputDirs, state, reloadConsumer)
            chain?.doFilter(req, res)
        }
        state.jettyInternal.servletContextHandlerConsumers.add { handler ->
            handler.addFilter(FilterHolder(reloadFilter), "/*", EnumSet.of(DispatcherType.REQUEST))
        }
    }

    @Synchronized
    private fun checkAndReload(
        watcher: DevReloadWatcher, compiler: DevReloadCompiler,
        sourcePathSet: Set<Path>, classOutputDirs: List<Path>,
        state: JavalinState, reloadConsumer: Consumer<JavalinConfig>
    ) {
        val changedFiles = watcher.checkForChanges()
        if (changedFiles.isEmpty()) return

        val sourceChanges = changedFiles.filter { file ->
            (file.toString().endsWith(".java") || file.toString().endsWith(".kt"))
                && sourcePathSet.any { file.startsWith(it) }
        }
        val classChanges = changedFiles.filter { file ->
            (file.toString().endsWith(".class") || file.toString().endsWith(".properties"))
                && sourcePathSet.none { file.startsWith(it) }
        }
        if (sourceChanges.isEmpty() && classChanges.isEmpty()) return

        val names = (sourceChanges + classChanges).map { it.fileName }
        logBasic("DevReloadPlugin: Changes detected in $names")

        // Compile if source files changed
        if (sourceChanges.isNotEmpty()) {
            val compileMs = compiler.compile(sourceChanges, pluginConfig.compileCommand)
            if (compileMs < 0) return // compilation failed
        }

        val result = reload(state, reloadConsumer, classOutputDirs, useClassReloading = true)
        if (sourceChanges.isNotEmpty()) watcher.resetBaseline() // don't re-trigger on freshly compiled .class files

        if (result != null) {
            val prefix = if (sourceChanges.isNotEmpty()) "Recompiled and reloaded" else "Reloaded"
            logBasic("DevReloadPlugin: $prefix in ${result.reloadMs}ms (${result.httpHandlers} handlers, ${result.wsHandlers} ws handlers)")
        }
    }

    internal data class ReloadResult(val reloadMs: Long, val httpHandlers: Int, val wsHandlers: Int)

    internal fun reload(
        state: JavalinState, reloadConsumer: Consumer<JavalinConfig>,
        classOutputDirs: List<Path> = pluginConfig.classOutputPaths.ifEmpty { detectClasspathDirectories() },
        useClassReloading: Boolean = false
    ): ReloadResult? {
        try {
            val startTime = System.currentTimeMillis()
            DevReloadReflection.resetAllRoutes(state)
            addValidationExceptionMapper(state)

            DevReloadReflection.withPluginReloadingEnabled(state) {
                val consumer = if (useClassReloading) {
                    val (c, cl) = DevReloadReflection.reloadConsumerFromDisk(
                        reloadConsumer, originalLambdaInfo, classOutputDirs, previousClassLoader, ::logExtensive
                    )
                    previousClassLoader = cl
                    c
                } else reloadConsumer
                consumer.accept(DevReloadReflection.createJavalinConfig(state))
            }

            return ReloadResult(
                System.currentTimeMillis() - startTime,
                state.internalRouter.allHttpHandlers().size,
                state.internalRouter.allWsHandlers().size
            )
        } catch (e: Exception) {
            JavalinLogger.error("DevReloadPlugin: Reload failed — routes may be inconsistent", e)
            return null
        }
    }

    companion object {
        internal fun detectClasspathDirectories(): List<Path> {
            val sep = FileSystems.getDefault().separator
            return System.getProperty("java.class.path", "")
                .split(System.getProperty("path.separator", ":"))
                .map { Path.of(it) }
                .filter { Files.isDirectory(it) }
                .filter { s -> val p = s.toString(); p.contains("target${sep}classes") || p.contains("target${sep}test-classes") || p.contains("build${sep}classes") }
                .distinct()
        }

        internal fun detectSourceDirectories(): List<Path> {
            val sourceDirs = mutableListOf<Path>()
            for (classDir in detectClasspathDirectories()) {
                val moduleRoot = when {
                    classDir.toString().contains("target") -> classDir.parent?.parent
                    classDir.toString().contains("build") -> classDir.let { var p = it; while (p.parent != null && p.fileName.toString() != "build") p = p.parent; p.parent }
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
    }
}
