package io.javalin.util

import java.io.File
import java.nio.file.*
import java.nio.file.StandardWatchEventKinds.*
import java.util.concurrent.TimeUnit

/**
 * Development server launcher that provides automatic compilation and restart.
 * 
 * This is a command-line tool that watches for source file changes, automatically
 * compiles them using Maven, and restarts the application.
 * 
 * **Usage:**
 * ```bash
 * # From your Maven project directory:
 * java -cp javalin-x.x.x.jar io.javalin.util.DevServer com.example.MyApp
 * ```
 * 
 * **What it does:**
 * 1. Watches src/main/java and src/main/resources for changes
 * 2. When changes detected, runs `mvn compile`
 * 3. Kills the running application process
 * 4. Starts a new application process
 * 
 * **Requirements:**
 * - Maven project structure (src/main/java, pom.xml)
 * - Maven command available in PATH
 * - Main class that starts a Javalin server
 * 
 * **No code changes needed** - Your application code remains unchanged.
 */
object DevServer {
    
    private var appProcess: Process? = null
    private val shouldRun = java.util.concurrent.atomic.AtomicBoolean(true)
    
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: java -cp javalin.jar io.javalin.util.DevServer <main-class> [args...]")
            println("Example: java -cp javalin.jar io.javalin.util.DevServer com.example.MyApp")
            System.exit(1)
        }
        
        val mainClass = args[0]
        val appArgs = args.drop(1).toTypedArray()
        
        println("=".repeat(70))
        println("Javalin Development Server")
        println("=".repeat(70))
        println("Main class: $mainClass")
        println("Watching: src/main/java, src/main/resources")
        println("Press Ctrl+C to stop")
        println("=".repeat(70))
        println()
        
        // Initial compilation and start
        if (!compile()) {
            println("ERROR: Initial compilation failed")
            System.exit(1)
        }
        startApp(mainClass, appArgs)
        
        // Watch for changes
        watchForChanges(mainClass, appArgs)
    }
    
    private fun compile(): Boolean {
        println("[DevServer] Compiling...")
        try {
            val process = ProcessBuilder("mvn", "compile", "-q")
                .inheritIO()
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                println("[DevServer] Compilation successful")
                return true
            } else {
                println("[DevServer] Compilation failed with exit code $exitCode")
                return false
            }
        } catch (e: Exception) {
            println("[DevServer] Compilation error: ${e.message}")
            return false
        }
    }
    
    private fun startApp(mainClass: String, args: Array<String>) {
        println("[DevServer] Starting application: $mainClass")
        
        try {
            // Build classpath from Maven
            val classpathProcess = ProcessBuilder("mvn", "dependency:build-classpath", "-q", "-DincludeScope=runtime")
                .start()
            val classpath = classpathProcess.inputStream.bufferedReader().use { it.readText().trim() }
            classpathProcess.waitFor()
            
            // Add target/classes to classpath
            val fullClasspath = "target/classes:$classpath"
            
            // Start the application
            val command = mutableListOf("java", "-cp", fullClasspath, mainClass)
            command.addAll(args)
            
            appProcess = ProcessBuilder(command)
                .inheritIO()
                .start()
            
            println("[DevServer] Application started (PID: ${appProcess?.pid()})")
        } catch (e: Exception) {
            println("[DevServer] Failed to start application: ${e.message}")
        }
    }
    
    private fun stopApp() {
        appProcess?.let {
            if (it.isAlive) {
                println("[DevServer] Stopping application...")
                it.destroy()
                if (!it.waitFor(5, TimeUnit.SECONDS)) {
                    println("[DevServer] Force killing application...")
                    it.destroyForcibly()
                }
                println("[DevServer] Application stopped")
            }
        }
    }
    
    private fun watchForChanges(mainClass: String, args: Array<String>) {
        val watchService = FileSystems.getDefault().newWatchService()
        val watchKeys = mutableMapOf<WatchKey, Path>()
        
        // Register directories to watch
        val dirsToWatch = listOf("src/main/java", "src/main/resources", "src/main/kotlin")
        for (dirPath in dirsToWatch) {
            val path = Paths.get(dirPath)
            if (Files.exists(path) && Files.isDirectory(path)) {
                registerRecursive(path, watchService, watchKeys)
                println("[DevServer] Watching: $dirPath")
            }
        }
        
        if (watchKeys.isEmpty()) {
            println("[DevServer] WARNING: No source directories found to watch")
            return
        }
        
        var lastChangeTime = 0L
        val debounceMs = 1000L
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            shouldRun.set(false)
            stopApp()
            println("\n[DevServer] Shutdown complete")
        })
        
        while (shouldRun.get()) {
            val key = watchService.poll(100, TimeUnit.MILLISECONDS) ?: continue
            
            val dir = watchKeys[key]
            if (dir == null) {
                key.cancel()
                continue
            }
            
            var hasChanges = false
            for (event in key.pollEvents()) {
                val kind = event.kind()
                if (kind == OVERFLOW) continue
                
                @Suppress("UNCHECKED_CAST")
                val filename = (event as WatchEvent<Path>).context()
                val changed = dir.resolve(filename)
                
                // Only react to Java/Kotlin/resource file changes
                if (changed.toString().matches(Regex(".*\\.(java|kt|properties|xml|yaml|yml|json)$"))) {
                    println("[DevServer] Detected change: $changed")
                    hasChanges = true
                    lastChangeTime = System.currentTimeMillis()
                }
            }
            
            // Reset the key
            if (!key.reset()) {
                watchKeys.remove(key)
                if (watchKeys.isEmpty()) break
            }
            
            // Check if we should trigger restart
            if (hasChanges && System.currentTimeMillis() - lastChangeTime >= debounceMs) {
                println("\n" + "=".repeat(70))
                println("Changes detected - recompiling and restarting...")
                println("=".repeat(70))
                
                stopApp()
                
                if (compile()) {
                    startApp(mainClass, args)
                } else {
                    println("[DevServer] Skipping restart due to compilation errors")
                    println("[DevServer] Fix the errors and save again to retry")
                }
                
                println()
                lastChangeTime = 0
            }
        }
        
        watchService.close()
    }
    
    private fun registerRecursive(start: Path, watchService: WatchService, watchKeys: MutableMap<WatchKey, Path>) {
        Files.walk(start).forEach { path ->
            if (Files.isDirectory(path)) {
                try {
                    val key = path.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE)
                    watchKeys[key] = path
                } catch (e: Exception) {
                    // Ignore directories we can't watch
                }
            }
        }
    }
}
