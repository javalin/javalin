package io.javalin.util

import io.javalin.Javalin
import io.javalin.util.JavalinLogger
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Development mode utility that provides automatic restart on file changes.
 * 
 * This utility watches for changes in compiled class files and automatically
 * restarts the Javalin server. It keeps the JVM alive and handles the restart lifecycle.
 * 
 * **Usage:**
 * ```java
 * public static void main(String[] args) {
 *     JavalinDevMode.runWithAutoRestart(() -> {
 *         return Javalin.create(config -> {
 *             config.bundledPlugins.enableDevLogging();
 *         })
 *         .get("/", ctx -> ctx.result("Hello World"))
 *         .start(7070);
 *     });
 * }
 * ```
 * 
 * **Requirements:**
 * Run your build tool in continuous compilation mode:
 * - Maven: `mvn compile -Ddev` (in a separate terminal)
 * - Gradle: `gradle classes --continuous` (in a separate terminal)
 * 
 * The utility will:
 * 1. Start your Javalin app
 * 2. Watch for .class file changes in target/classes or build/classes
 * 3. Stop and restart the app when changes are detected
 * 4. Keep the JVM alive with a non-daemon thread
 */
object JavalinDevMode {

    private val lock = ReentrantLock()
    private val shouldRun = AtomicBoolean(true)
    private var currentApp: Javalin? = null
    private var appFactory: (() -> Javalin)? = null
    private var lastRestartTime = 0L
    
    /**
     * Runs the Javalin app with automatic restart on file changes.
     * This method blocks and keeps the JVM alive.
     * 
     * @param factory Function that creates and starts a new Javalin instance
     */
    @JvmStatic
    fun runWithAutoRestart(factory: () -> Javalin) {
        appFactory = factory
        
        // Detect class directories
        val classDirs = detectClassDirectories()
        if (classDirs.isEmpty()) {
            JavalinLogger.warn("JavalinDevMode: No class directories found (target/classes or build/classes)")
            JavalinLogger.warn("JavalinDevMode: Make sure you're in a Maven or Gradle project root")
            JavalinLogger.info("JavalinDevMode: Starting app without file watching...")
            factory()
            keepAlive()
            return
        }
        
        JavalinLogger.info("JavalinDevMode: Starting in development mode")
        JavalinLogger.info("JavalinDevMode: Watching: $classDirs")
        JavalinLogger.info("JavalinDevMode: Run 'mvn compile -Ddev' or 'gradle classes --continuous' for automatic recompilation")
        
        // Start the app
        restartApp()
        
        // Start file watcher in background thread
        val watcherThread = Thread({
            watchForChanges(classDirs)
        }, "javalin-devmode-watcher")
        watcherThread.isDaemon = true
        watcherThread.start()
        
        // Keep main thread alive
        keepAlive()
    }
    
    private fun detectClassDirectories(): List<String> {
        val candidates = listOf(
            "target/classes",
            "build/classes/java/main",
            "build/classes/kotlin/main"
        )
        return candidates.filter { Files.exists(Paths.get(it)) && Files.isDirectory(Paths.get(it)) }
    }
    
    private fun restartApp() {
        lock.withLock {
            try {
                // Stop old app
                currentApp?.let {
                    JavalinLogger.info("JavalinDevMode: Stopping server...")
                    it.stop()
                }
                
                // Start new app
                JavalinLogger.info("JavalinDevMode: Starting server...")
                currentApp = appFactory!!()
                lastRestartTime = System.currentTimeMillis()
                
                JavalinLogger.info("JavalinDevMode: Server started successfully")
            } catch (e: Exception) {
                JavalinLogger.error("JavalinDevMode: Failed to restart server", e)
            }
        }
    }
    
    private fun watchForChanges(directories: List<String>) {
        try {
            val watchService = FileSystems.getDefault().newWatchService()
            val watchKeys = mutableMapOf<WatchKey, Path>()
            
            // Register directories
            for (dir in directories) {
                try {
                    val path = Paths.get(dir)
                    val key = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                    watchKeys[key] = path
                } catch (e: Exception) {
                    JavalinLogger.error("JavalinDevMode: Failed to watch directory: $dir", e)
                }
            }
            
            var lastChangeTime = 0L
            val debounceMs = 500L
            val minRestartInterval = 2000L
            
            while (shouldRun.get()) {
                val key = watchService.poll(100, TimeUnit.MILLISECONDS) ?: continue
                
                val dir = watchKeys[key]
                if (dir == null) {
                    key.cancel()
                    continue
                }
                
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind == OVERFLOW) continue
                    
                    @Suppress("UNCHECKED_CAST")
                    val filename = (event as WatchEvent<Path>).context()
                    val changed = dir.resolve(filename)
                    
                    // Only react to .class file changes
                    if (changed.toString().endsWith(".class")) {
                        JavalinLogger.info("JavalinDevMode: Detected change: $changed")
                        lastChangeTime = System.currentTimeMillis()
                    }
                }
                
                // Reset the key
                if (!key.reset()) {
                    watchKeys.remove(key)
                    if (watchKeys.isEmpty()) break
                }
                
                // Check if we should trigger restart
                val timeSinceChange = System.currentTimeMillis() - lastChangeTime
                val timeSinceRestart = System.currentTimeMillis() - lastRestartTime
                
                if (lastChangeTime > 0 && 
                    timeSinceChange >= debounceMs && 
                    timeSinceRestart >= minRestartInterval) {
                    
                    lastChangeTime = 0
                    restartApp()
                }
            }
            
            watchService.close()
        } catch (e: InterruptedException) {
            JavalinLogger.info("JavalinDevMode: File watcher stopped")
        } catch (e: Exception) {
            JavalinLogger.error("JavalinDevMode: Error in file watcher", e)
        }
    }
    
    private fun keepAlive() {
        // Keep JVM alive
        val keepAliveThread = Thread({
            try {
                while (shouldRun.get()) {
                    Thread.sleep(1000)
                }
            } catch (e: InterruptedException) {
                // Exit
            }
        }, "javalin-devmode-keepalive")
        keepAliveThread.isDaemon = false  // Non-daemon to keep JVM alive
        keepAliveThread.start()
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
        
        // Wait for keepalive thread
        try {
            keepAliveThread.join()
        } catch (e: InterruptedException) {
            // Exit
        }
    }
    
    /**
     * Stops the development mode and shuts down the server.
     */
    @JvmStatic
    fun shutdown() {
        shouldRun.set(false)
        currentApp?.stop()
    }
}
