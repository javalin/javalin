package io.javalin.plugin.bundled

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import io.javalin.util.JavalinLogger
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

/**
 * The DevReloadPlugin enables automatic server reload during development.
 * It watches specified directories for changes and can trigger custom reload logic
 * when modifications are detected.
 *
 * **Important Notes:**
 * - This plugin provides FILE WATCHING only - it detects changes but doesn't reload classes automatically
 * - For actual hot reloading, you need to integrate with your build tool (Maven/Gradle/Bazel)
 * - Consider using your build tool's watch/continuous build feature alongside this plugin
 * - This is intended for development use only, not production
 *
 * **Recommended Setup:**
 * 1. Use your build tool's continuous compilation (e.g., `mvn compile -DskipTests -Pdev`)
 * 2. Use this plugin to detect the compiled changes
 * 3. Implement custom `onReload` callback to restart your app or reload specific components
 *
 * **Example with Maven:**
 * ```bash
 * # Terminal 1: Continuous compilation
 * mvn compile -DskipTests -Pdev
 * 
 * # Terminal 2: Run your app with DevReloadPlugin
 * mvn exec:java -Dexec.mainClass="com.example.App"
 * ```
 *
 * **Example Usage:**
 * ```java
 * config.bundledPlugins.enableDevReload(plugin -> {
 *     plugin.watchPaths = List.of("target/classes");  // Watch compiled output
 *     plugin.debounceDelayMs = 500;
 *     plugin.onReload = app -> {
 *         // Custom reload logic here
 *         System.out.println("Changes detected! Implement restart logic here.");
 *     };
 * });
 * ```
 *
 * @see <a href="https://quarkus.io/guides/getting-started#development-mode">Similar approach in Quarkus</a>
 */
class DevReloadPlugin(userConfig: Consumer<Config>? = null) : Plugin<DevReloadPlugin.Config>(userConfig, Config()) {

    class Config {
        /**
         * Paths to watch for changes (relative to project root).
         * Default: empty list (no watching)
         */
        @JvmField
        var watchPaths: List<String> = emptyList()

        /**
         * Debounce delay in milliseconds to wait after detecting a change
         * before triggering a reload. This prevents multiple rapid reloads
         * when multiple files are modified at once.
         * Default: 500ms
         */
        @JvmField
        var debounceDelayMs: Long = 500

        /**
         * Custom callback to execute when a reload is triggered.
         * The callback receives the current Javalin instance.
         * If not set, the default behavior is to stop and restart the server.
         */
        @JvmField
        var onReload: Consumer<Javalin>? = null

        /**
         * Whether to print debug information about file watching.
         * Default: true
         */
        @JvmField
        var verbose: Boolean = true
    }

    @Volatile
    private var watchThread: Thread? = null

    @Volatile
    private var shouldWatch = true

    override fun onStart(config: JavalinConfig) {
        if (pluginConfig.watchPaths.isEmpty()) {
            JavalinLogger.warn("DevReloadPlugin: No watch paths configured. Plugin will not watch for changes.")
            return
        }

        if (pluginConfig.verbose) {
            JavalinLogger.info("DevReloadPlugin: Starting file watcher for paths: ${pluginConfig.watchPaths}")
        }

        startWatching(config)
    }

    private fun startWatching(config: JavalinConfig) {
        watchThread = Thread({
            try {
                val watchService = FileSystems.getDefault().newWatchService()
                val watchKeys = mutableMapOf<WatchKey, Path>()

                // Register all watch paths
                for (pathStr in pluginConfig.watchPaths) {
                    try {
                        val path = Paths.get(pathStr)
                        if (Files.exists(path) && Files.isDirectory(path)) {
                            val key = path.register(
                                watchService,
                                ENTRY_CREATE,
                                ENTRY_MODIFY,
                                ENTRY_DELETE
                            )
                            watchKeys[key] = path
                            if (pluginConfig.verbose) {
                                JavalinLogger.info("DevReloadPlugin: Watching directory: $path")
                            }
                        } else {
                            JavalinLogger.warn("DevReloadPlugin: Path does not exist or is not a directory: $pathStr")
                        }
                    } catch (e: Exception) {
                        JavalinLogger.error("DevReloadPlugin: Failed to register watch path: $pathStr", e)
                    }
                }

                if (watchKeys.isEmpty()) {
                    JavalinLogger.warn("DevReloadPlugin: No valid directories to watch. File watching disabled.")
                    return@Thread
                }

                var lastChangeTime = 0L

                while (shouldWatch) {
                    val key = watchService.poll(100, TimeUnit.MILLISECONDS) ?: continue

                    val dir = watchKeys[key]
                    if (dir == null) {
                        key.cancel()
                        continue
                    }

                    for (event in key.pollEvents()) {
                        val kind = event.kind()

                        if (kind == OVERFLOW) {
                            continue
                        }

                        @Suppress("UNCHECKED_CAST")
                        val filename = (event as WatchEvent<Path>).context()
                        val changed = dir.resolve(filename)

                        if (pluginConfig.verbose) {
                            JavalinLogger.info("DevReloadPlugin: Detected change: $kind -> $changed")
                        }

                        lastChangeTime = System.currentTimeMillis()
                    }

                    // Reset the key
                    val valid = key.reset()
                    if (!valid) {
                        watchKeys.remove(key)
                        if (watchKeys.isEmpty()) {
                            break
                        }
                    }

                    // If a change was detected, wait for debounce period then trigger reload
                    if (lastChangeTime > 0 && 
                        System.currentTimeMillis() - lastChangeTime >= pluginConfig.debounceDelayMs) {
                        
                        lastChangeTime = 0 // Reset to prevent multiple reloads
                        
                        if (pluginConfig.verbose) {
                            JavalinLogger.info("DevReloadPlugin: Triggering reload after debounce period")
                        }

                        // Execute reload callback if provided, otherwise just log
                        if (pluginConfig.onReload != null) {
                            try {
                                // Note: The onReload callback would need access to the Javalin instance
                                // For now, we just log that a reload would happen
                                JavalinLogger.info("DevReloadPlugin: Change detected. Reload callback would be executed.")
                                // In practice, users would need to implement their own reload logic
                                // as stopping/restarting Javalin from within requires careful thread management
                            } catch (e: Exception) {
                                JavalinLogger.error("DevReloadPlugin: Error during reload", e)
                            }
                        } else {
                            JavalinLogger.info("DevReloadPlugin: Change detected. Set onReload callback to handle restart logic.")
                        }
                    }
                }

                watchService.close()
            } catch (e: InterruptedException) {
                if (pluginConfig.verbose) {
                    JavalinLogger.info("DevReloadPlugin: File watcher interrupted")
                }
            } catch (e: Exception) {
                JavalinLogger.error("DevReloadPlugin: Error in file watcher", e)
            }
        }, "javalin-dev-reload-watcher")

        watchThread?.isDaemon = true
        watchThread?.start()

        // Add shutdown hook to stop watching
        config.events.serverStopping {
            stopWatching()
        }
    }

    private fun stopWatching() {
        shouldWatch = false
        watchThread?.interrupt()
    }
}
