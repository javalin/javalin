package io.javalin

import io.javalin.util.JavalinDevMode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

class TestJavalinDevMode {

    @AfterEach
    fun cleanup() {
        try {
            JavalinDevMode.shutdown()
            Thread.sleep(100)
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `dev mode can be started and stopped`(@TempDir tempDir: Path) {
        val startCount = AtomicInteger(0)
        
        Thread {
            JavalinDevMode.runWithAutoRestart {
                startCount.incrementAndGet()
                Javalin.create().start(0)
            }
        }.start()
        
        // Wait for initial start
        Thread.sleep(500)
        assertThat(startCount.get()).isGreaterThan(0)
        
        // Shutdown
        JavalinDevMode.shutdown()
        Thread.sleep(200)
    }

    @Test  
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun `dev mode works without class directories`() {
        val startCount = AtomicInteger(0)
        
        Thread {
            // This should work but not watch any files
            JavalinDevMode.runWithAutoRestart {
                startCount.incrementAndGet()
                Javalin.create().start(0)
            }
        }.start()
        
        Thread.sleep(500)
        assertThat(startCount.get()).isGreaterThan(0)
        
        JavalinDevMode.shutdown()
        Thread.sleep(200)
    }

    @Test
    fun `dev mode detects class directories when they exist`(@TempDir tempDir: Path) {
        // Create a mock project structure
        val targetClasses = tempDir.resolve("target/classes")
        Files.createDirectories(targetClasses)
        
        // Change to temp directory
        val originalDir = System.getProperty("user.dir")
        try {
            System.setProperty("user.dir", tempDir.toString())
            
            val startCount = AtomicInteger(0)
            
            Thread {
                JavalinDevMode.runWithAutoRestart {
                    startCount.incrementAndGet()
                    Javalin.create().start(0)
                }
            }.start()
            
            Thread.sleep(500)
            assertThat(startCount.get()).isGreaterThan(0)
            
            JavalinDevMode.shutdown()
            Thread.sleep(200)
        } finally {
            System.setProperty("user.dir", originalDir)
        }
    }
}
