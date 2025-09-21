package io.javalin.staticfiles

import io.javalin.http.staticfiles.AliasCheck
import io.javalin.http.staticfiles.NativeConfigurableHandler
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import org.junit.jupiter.api.Test
import java.io.File

class DebugAliasTest {

    @Test
    fun `debug alias handling`() {
        val tempDir = File.createTempFile("test", "").apply {
            delete()
            mkdirs()
        }
        
        println("Temp dir: ${tempDir.absolutePath}")
        
        // Create a symlink
        val symlinkPath = File(tempDir, "linked.txt")
        val targetPath = File("src/test/external/txt.txt").absolutePath
        
        println("Creating symlink from ${symlinkPath.absolutePath} to $targetPath")
        
        try {
            java.nio.file.Files.createSymbolicLink(
                symlinkPath.toPath(),
                java.nio.file.Paths.get(targetPath)
            )
            
            val aliasCheck = object : AliasCheck {
                override fun checkAlias(path: String, resource: io.javalin.http.staticfiles.NativeResource): Boolean {
                    println("Alias check called for path: $path, resource: $resource")
                    return true // Allow all for debugging
                }
            }
            
            val config = StaticFileConfig().apply {
                directory = tempDir.absolutePath
                location = Location.EXTERNAL
                nativeAliasCheck = aliasCheck
            }
            
            val handler = NativeConfigurableHandler(config)
            
            val resource = handler.getResource("linked.txt")
            println("Resource found: $resource")
            if (resource != null) {
                println("Resource exists: ${resource.exists}")
                println("Resource isAlias: ${resource.isAlias}")
                println("Resource content: ${resource.newInputStream()?.use { String(it.readAllBytes()) }}")
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempDir.deleteRecursively()
        }
    }
}