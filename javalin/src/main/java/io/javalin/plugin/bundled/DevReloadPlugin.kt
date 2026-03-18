package io.javalin.plugin.bundled

import io.javalin.config.JavalinConfig
import io.javalin.config.JavalinState
import io.javalin.plugin.Plugin
import io.javalin.util.JavalinLogger
import io.javalin.validation.Validation.Companion.addValidationExceptionMapper
import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import java.net.URLClassLoader
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.EnumSet
import java.util.function.Consumer
import org.eclipse.jetty.ee10.servlet.FilterHolder

/**
 * Development-mode plugin that reloads routes when source or class files change.
 *
 * Rather than polling in a background thread, the plugin checks for file changes
 * on each incoming HTTP request. If changes are detected, it compiles (if source files changed)
 * and re-executes the original config consumer from [io.javalin.Javalin.create]
 * before the request is handled. The Jetty server continues running on the same port
 * throughout — only the routing tables are swapped.
 *
 * This means zero CPU when idle, and the first request after a code change pays the
 * compile+reload cost while subsequent requests are unaffected until the next change.
 *
 * This plugin is intended for development use only.
 *
 * Usage:
 * ```java
 * Javalin.create(config -> {
 *     config.routes.get("/hello", ctx -> ctx.result("Hello!"));
 *     config.bundledPlugins.enableDevReload();
 * }).start(7070);
 * ```
 */
class DevReloadPlugin(userConfig: Consumer<Config>? = null) : Plugin<DevReloadPlugin.Config>(userConfig, Config()) {

    // The name of the lambda method that backs the original Javalin.create() consumer.
    // Captured at construction time (inside enableDevReload()) while the user's config lambda is still on the stack.
    private val originalLambdaMethodName: String? = StackWalker.getInstance().walk { frames ->
        frames.filter { frame -> frame.methodName.contains("lambda") }
            .map { it.methodName }
            .findFirst()
            .orElse(null)
    }

    // Track the previous classloader to close it on the next reload (prevents file handle leaks)
    private var previousClassLoader: URLClassLoader? = null

    /** Controls how much the DevReloadPlugin logs. */
    enum class LogLevel {
        /** No logging at all. */
        NONE,
        /** Logs file change detection, compile, and reload summary — one or two lines per reload. */
        BASIC,
        /** Logs every file system event, classloader details, and full diagnostics. */
        EXTENSIVE
    }

    class Config {
        /**
         * Directories to watch for **source file** changes (.java, .kt).
         * When source files change, the plugin compiles them automatically before reloading.
         * If empty (the default), the plugin auto-detects source directories relative to the
         * class output directories (e.g., `src/main/java`, `src/test/java`).
         *
         * Set explicitly for non-standard project layouts:
         * ```java
         * reload.sourceWatchPaths = List.of(Path.of("app/src"));
         * ```
         */
        @JvmField
        var sourceWatchPaths: List<Path> = emptyList()

        /**
         * Directories to watch for **compiled class file** changes (.class, .properties).
         * When class files change (e.g., from an external IDE build), the plugin reloads
         * without recompiling. If empty (the default), the plugin auto-detects class output
         * directories from the classpath (e.g., `target/classes`, `build/classes`).
         *
         * Set explicitly for non-standard project layouts:
         * ```java
         * reload.classWatchPaths = List.of(Path.of("out/production/classes"));
         * ```
         */
        @JvmField
        var classWatchPaths: List<Path> = emptyList()

        /**
         * Directories that the fresh classloader uses to load updated `.class` files on reload.
         * If empty (the default), uses the same directories as [classWatchPaths].
         *
         * Set explicitly if your class output directory differs from the watch directory, or if
         * you need to include additional directories (e.g., annotation processor output):
         * ```java
         * reload.classOutputPaths = List.of(
         *     Path.of("target/classes"),
         *     Path.of("target/generated-sources/annotations")
         * );
         * ```
         */
        @JvmField
        var classOutputPaths: List<Path> = emptyList()

        /**
         * Optional override for the config consumer to re-execute on reload.
         * If null (the default), the original consumer passed to `Javalin.create()`
         * is re-used automatically.
         */
        @JvmField
        var onReload: Consumer<JavalinConfig>? = null

        /**
         * Custom shell command to run for compilation when source files change.
         * If null (the default), the plugin uses the in-process Java compiler (`javax.tools`).
         *
         * When set, the plugin runs this command via the system shell on source file changes.
         * The command is responsible for compiling **all** necessary files (not just the changed ones),
         * because it may need to run annotation processors, compiler plugins, or other build steps.
         *
         * Use this for projects that need:
         * - Annotation processors (Lombok, MapStruct, Dagger, etc.)
         * - Compiler plugins (Error Prone, Checker Framework, etc.)
         * - Non-javac compilers (Eclipse compiler, kotlinc, etc.)
         * - Multi-module builds
         * - Any build tool rules that javac alone cannot replicate
         *
         * Examples:
         * ```java
         * reload.compileCommand = "mvn compile -o -q --batch-mode";
         * reload.compileCommand = "gradle classes -q";
         * reload.compileCommand = "./gradlew :app:classes -q";  // multi-module
         * ```
         */
        @JvmField
        var compileCommand: String? = null

        /**
         * Controls the amount of logging output.
         * - [LogLevel.NONE] — silent
         * - [LogLevel.BASIC] — one or two lines per reload (default)
         * - [LogLevel.EXTENSIVE] — every file event, classloader details, full diagnostics
         */
        @JvmField
        var logging: LogLevel = LogLevel.BASIC
    }

    private fun logBasic(msg: String) {
        if (pluginConfig.logging >= LogLevel.BASIC) JavalinLogger.info(msg)
    }

    private fun logExtensive(msg: String) {
        if (pluginConfig.logging >= LogLevel.EXTENSIVE) JavalinLogger.info(msg)
    }

    override fun onStart(state: JavalinState) {
        val reloadConsumer = pluginConfig.onReload ?: state.userConfig

        if (reloadConsumer == null) {
            JavalinLogger.warn("DevReloadPlugin: No config consumer available — plugin disabled.")
            return
        }

        val classPaths = pluginConfig.classWatchPaths.ifEmpty { detectClasspathDirectories() }
            .map { it.toAbsolutePath().normalize() }
            .filter { Files.isDirectory(it) }
        val sourcePaths = pluginConfig.sourceWatchPaths.ifEmpty { detectSourceDirectories() }
            .map { it.toAbsolutePath().normalize() }
            .filter { Files.isDirectory(it) }

        val allWatchPaths = (sourcePaths + classPaths).distinct()
        if (allWatchPaths.isEmpty()) {
            JavalinLogger.warn("DevReloadPlugin: No directories to watch. Make sure your project is compiled.")
            return
        }

        logExtensive("DevReloadPlugin: Working directory: ${Path.of("").toAbsolutePath()}")
        logExtensive("DevReloadPlugin: Source paths: $sourcePaths")
        logExtensive("DevReloadPlugin: Class paths: $classPaths")

        val sourcePathSet = sourcePaths.toSet()
        val watcher = DevReloadWatcher(allWatchPaths)
        val classOutputDirs = pluginConfig.classOutputPaths.ifEmpty { detectClasspathDirectories() }
        val compiler = DevReloadCompiler(classOutputDirs, ::logExtensive)

        JavalinLogger.startup("DevReloadPlugin: Watching for changes (on-request)")

        // Register a Jetty servlet filter that runs before the Javalin servlet.
        // This is completely outside Javalin's request lifecycle, so it doesn't
        // interfere with user-defined lifecycles or get cleared on reload.
        val reloadFilter = Filter { request: ServletRequest?, response: ServletResponse?, chain: FilterChain? ->
            checkAndReload(watcher, compiler, sourcePathSet, state, reloadConsumer)
            chain?.doFilter(request, response)
        }
        state.jettyInternal.servletContextHandlerConsumers.add { handler ->
            handler.addFilter(FilterHolder(reloadFilter), "/*", EnumSet.of(DispatcherType.REQUEST))
        }
    }

    /**
     * Called on every incoming request. Checks for file changes and reloads if needed.
     * Source file changes trigger compile + reload. Class-only changes (external build) just reload.
     * Synchronized so concurrent requests don't trigger parallel reloads.
     */
    @Synchronized
    private fun checkAndReload(
        watcher: DevReloadWatcher,
        compiler: DevReloadCompiler,
        sourcePathSet: Set<Path>,
        state: JavalinState,
        reloadConsumer: Consumer<JavalinConfig>
    ) {
        val changedFiles = watcher.findChanges()
        if (changedFiles.isEmpty()) return

        // Split changes into source files and class/resource files
        val sourceChanges = changedFiles.filter { file ->
            (file.toString().endsWith(".java") || file.toString().endsWith(".kt"))
                && sourcePathSet.any { file.startsWith(it) }
        }
        val classChanges = changedFiles.filter { file ->
            (file.toString().endsWith(".class") || file.toString().endsWith(".properties"))
                && sourcePathSet.none { file.startsWith(it) }
        }

        watcher.acceptChanges()

        if (sourceChanges.isNotEmpty()) {
            // Source files changed — compile then reload
            val names = sourceChanges.map { it.fileName }
            logBasic("DevReloadPlugin: Changes detected in $names")
            val compileMs = compiler.compile(sourceChanges, pluginConfig.compileCommand)
            if (compileMs >= 0) {
                val result = reload(state, reloadConsumer, useClassReloading = true)
                // Reset baseline so the .class files we just produced don't trigger a second reload
                watcher.resetBaseline()
                if (result != null) {
                    logBasic("DevReloadPlugin: Recompiled in ${compileMs}ms and reloaded in ${result.reloadMs}ms (${result.httpHandlers} handlers, ${result.wsHandlers} ws handlers)")
                }
            }
        } else if (classChanges.isNotEmpty()) {
            // Only class files changed (external compile) — just reload
            val names = classChanges.map { it.fileName }
            logBasic("DevReloadPlugin: Changes detected in $names")
            val result = reload(state, reloadConsumer, useClassReloading = true)
            if (result != null) {
                logBasic("DevReloadPlugin: Reloaded in ${result.reloadMs}ms (${result.httpHandlers} handlers, ${result.wsHandlers} ws handlers)")
            }
        }
    }

    internal data class ReloadResult(val reloadMs: Long, val httpHandlers: Int, val wsHandlers: Int)

    /**
     * Clears all routes and re-executes the config consumer using a fresh classloader
     * so that updated `.class` files are picked up from disk.
     * Returns reload stats on success, or null on failure.
     */
    internal fun reload(
        state: JavalinState,
        reloadConsumer: Consumer<JavalinConfig>,
        useClassReloading: Boolean = false
    ): ReloadResult? {
        try {
            val startTime = System.currentTimeMillis()

            DevReloadReflection.resetAllRoutes(state)
            addValidationExceptionMapper(state)

            DevReloadReflection.withPluginReloadingEnabled(state) {
                val consumer = if (useClassReloading) reloadConsumerFromDisk(reloadConsumer) else reloadConsumer
                val publicConfig = JavalinConfig(state)
                consumer.accept(publicConfig)
            }

            return ReloadResult(
                reloadMs = System.currentTimeMillis() - startTime,
                httpHandlers = state.internalRouter.allHttpHandlers().size,
                wsHandlers = state.internalRouter.allWsHandlers().size
            )
        } catch (e: Exception) {
            JavalinLogger.error("DevReloadPlugin: Reload failed — routes may be inconsistent", e)
            return null
        }
    }

    /**
     * Creates a fresh classloader that reloads application classes from disk,
     * then re-invokes the config consumer's enclosing class to get updated lambda bytecode.
     *
     * The classloader uses child-first loading: any class found in the class output directories
     * is loaded from disk (fresh), otherwise falls back to the parent classloader. This means
     * controllers, services, and other application classes get the latest compiled version,
     * while framework/library classes from JARs are naturally delegated to the parent.
     *
     * Only JDK and core runtime classes are forced to parent to preserve type identity.
     */
    private fun reloadConsumerFromDisk(originalConsumer: Consumer<JavalinConfig>): Consumer<JavalinConfig> {
        try {
            val classOutputDirs = pluginConfig.classOutputPaths.ifEmpty { detectClasspathDirectories() }
            val classpathUrls = classOutputDirs
                .map { it.toAbsolutePath().normalize() }
                .filter { Files.isDirectory(it) }
                .map { it.toUri().toURL() }
                .toTypedArray()
            if (classpathUrls.isEmpty()) return originalConsumer

            val lambdaClassName = originalConsumer.javaClass.name
            // Java lambdas: ClassName$$Lambda$123/0x... -> ClassName
            // Kotlin lambdas: ClassName$$special$$inlined$... -> ClassName
            val enclosingClassName = if ("$$" in lambdaClassName) {
                lambdaClassName.substringBefore("$$")
            } else {
                lambdaClassName
            }

            logExtensive("DevReloadPlugin: Reloading $enclosingClassName via fresh classloader")

            // Close the previous classloader to prevent file handle leaks
            try { previousClassLoader?.close() } catch (_: Exception) {}

            // Child-first classloader: tries class output dirs first, falls back to parent.
            // Framework/library classes are always delegated to parent to preserve type identity
            // (e.g., JavalinConfig, Context, Handler must be the same Class objects).
            // The enclosing class of the consumer (and its inner classes) are always loaded
            // child-first so that the reloaded lambda picks up freshly compiled dependencies.
            val freshClassLoader = object : URLClassLoader(classpathUrls, this.javaClass.classLoader) {
                override fun loadClass(name: String, resolve: Boolean): Class<*> {
                    synchronized(getClassLoadingLock(name)) {
                        var c = findLoadedClass(name)
                        if (c == null) {
                            val isEnclosingClass = name == enclosingClassName || name.startsWith("$enclosingClassName$")
                            val delegateToParent = !isEnclosingClass && (
                                name.startsWith("io.javalin.") ||
                                name.startsWith("java.") || name.startsWith("javax.") ||
                                name.startsWith("jdk.") || name.startsWith("sun.") ||
                                name.startsWith("jakarta.") || name.startsWith("kotlin.") ||
                                name.startsWith("org.eclipse.jetty.") || name.startsWith("org.slf4j."))
                            c = if (delegateToParent) {
                                parent.loadClass(name)
                            } else {
                                // Child-first: load from class output dirs if available, otherwise parent
                                try { findClass(name) } catch (_: ClassNotFoundException) { parent.loadClass(name) }
                            }
                        }
                        if (resolve) resolveClass(c)
                        return c
                    }
                }
            }

            val reloadedClass = freshClassLoader.loadClass(enclosingClassName)
            previousClassLoader = freshClassLoader

            // Find the lambda method in the reloaded class that matches the original consumer.
            // We use the method name captured at registration time via StackWalker.
            var targetMethodName = originalLambdaMethodName
            logExtensive("DevReloadPlugin: Stored lambda method name: $targetMethodName")

            // If StackWalker didn't capture it, try SerializedLambda (works for Java lambdas)
            if (targetMethodName == null) {
                try {
                    val writeReplace = originalConsumer.javaClass.getDeclaredMethod("writeReplace")
                    writeReplace.isAccessible = true
                    val serializedLambda = writeReplace.invoke(originalConsumer) as java.lang.invoke.SerializedLambda
                    targetMethodName = serializedLambda.implMethodName
                    logExtensive("DevReloadPlugin: Found target method '$targetMethodName' via SerializedLambda")
                } catch (_: Exception) { /* Not serializable — continue */ }
            }

            // Find all config lambda methods (static, contains "lambda", takes JavalinConfig)
            val configLambdas = reloadedClass.declaredMethods.filter { method ->
                java.lang.reflect.Modifier.isStatic(method.modifiers)
                    && method.name.contains("lambda")
                    && method.parameterCount == 1
                    && method.parameterTypes[0].name == JavalinConfig::class.java.name
            }

            val targetMethod = if (targetMethodName != null) {
                configLambdas.find { it.name == targetMethodName }
            } else {
                // Last resort: use single match or first match
                configLambdas.singleOrNull() ?: configLambdas.firstOrNull()
            }

            if (targetMethod != null) {
                targetMethod.isAccessible = true
                logExtensive("DevReloadPlugin: Using lambda '${targetMethod.name}' from $enclosingClassName")
                return Consumer { config -> targetMethod.invoke(null, config) }
            }

            logExtensive("DevReloadPlugin: No config lambda found in $enclosingClassName, using original consumer")
            return originalConsumer
        } catch (e: Exception) {
            JavalinLogger.warn("DevReloadPlugin: Classloader reload failed, using original consumer", e)
            return originalConsumer
        }
    }

    companion object {
        internal fun detectClasspathDirectories(): List<Path> {
            val sep = FileSystems.getDefault().separator
            return System.getProperty("java.class.path", "")
                .split(System.getProperty("path.separator", ":"))
                .map { Path.of(it) }
                .filter { Files.isDirectory(it) }
                .filter { path ->
                    val str = path.toString()
                    str.contains("target${sep}classes") ||
                        str.contains("target${sep}test-classes") ||
                        str.contains("build${sep}classes")
                }
                .distinct()
        }

        internal fun detectSourceDirectories(): List<Path> {
            val classDirs = detectClasspathDirectories()
            val sourceDirs = mutableListOf<Path>()
            for (classDir in classDirs) {
                val moduleRoot = if (classDir.toString().contains("target")) {
                    classDir.parent?.parent
                } else if (classDir.toString().contains("build")) {
                    var p = classDir
                    while (p.parent != null && p.fileName.toString() != "build") p = p.parent
                    p.parent
                } else null

                if (moduleRoot != null) {
                    listOf("src/main/java", "src/main/kotlin", "src/test/java", "src/test/kotlin")
                        .map { moduleRoot.resolve(it) }
                        .filter { Files.isDirectory(it) }
                        .forEach { sourceDirs.add(it.toAbsolutePath().normalize()) }
                }
            }
            return sourceDirs.distinct()
        }
    }
}
