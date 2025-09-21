package io.javalin.staticfiles

import io.javalin.http.staticfiles.NativeConfigurableHandler
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import org.junit.jupiter.api.Test
import java.io.File

class DebugExternalTest {

    @Test
    fun `debug external path resolution`() {
        val config = StaticFileConfig().apply {
            directory = "src/test/external/"
            location = Location.EXTERNAL
        }
        
        println("Working directory: ${System.getProperty("user.dir")}")
        println("Config directory: ${config.directory}")
        
        val actualPath = java.nio.file.Paths.get(config.directory).toAbsolutePath()
        println("Resolved absolute path: $actualPath")
        println("Path exists: ${java.nio.file.Files.exists(actualPath)}")
        
        if (java.nio.file.Files.exists(actualPath)) {
            println("Files in directory:")
            java.nio.file.Files.list(actualPath).forEach { println("  $it") }
        }
        
        try {
            val handler = NativeConfigurableHandler(config)
            
            val resource = handler.getResource("html.html")
            println("Resource found: $resource")
            if (resource != null) {
                println("Resource exists: ${resource.exists}")
                println("Resource content: ${resource.newInputStream()?.use { String(it.readAllBytes()) }}")
            }
        } catch (e: Exception) {
            println("Exception creating handler: $e")
            e.printStackTrace()
        }
    }
}