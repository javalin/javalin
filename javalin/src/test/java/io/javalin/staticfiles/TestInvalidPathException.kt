package io.javalin.staticfiles

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestInvalidPathException {

    @Test
    fun `server does not crash on invalid path characters`() {
        TestUtil.test(
            Javalin.create { config ->
                config.staticFiles.add("/public", Location.CLASSPATH)
            }
        ) { _, http ->
            // This should not throw an exception even though the path contains invalid characters
            val response = http.get("/api/https://www.youtube.com/")
            // Should return 404 instead of throwing InvalidPathException
            assertThat(response.status).isIn(404, 405) // Either not found or method not allowed
        }
    }

    @Test
    fun `server handles paths with colons gracefully`() {
        TestUtil.test(
            Javalin.create { config ->
                config.staticFiles.add("/public", Location.CLASSPATH)
            }
        ) { _, http ->
            // Test various problematic paths (but valid URLs)
            val paths = listOf(
                "/invalid:path",
                "/test/file:with:colons.txt",
                "/api/https://example.com/"
            )
            
            paths.forEach { path ->
                val response = http.get(path)
                // Should not throw exception, just return 404 or similar
                assertThat(response.status).isIn(404, 405)
            }
        }
    }

    @Test
    fun `path validation blocks invalid characters`() {
        TestUtil.test(
            Javalin.create { config ->
                config.staticFiles.add("/public", Location.CLASSPATH)
            }
        ) { _, http ->
            // Test paths that should be blocked by path validation but might be valid URLs
            val invalidPaths = listOf(
                "/test<file.txt",   // < character
                "/test>file.txt",   // > character  
                "/test\"file.txt",  // " character
                "/test|file.txt"    // | character (note: this may fail at HTTP level)
            )
            
            invalidPaths.forEach { path ->
                try {
                    val response = http.get(path)
                    // If we get here, path validation should have returned 404
                    assertThat(response.status).isIn(404, 405)
                } catch (e: Exception) {
                    // Some characters may be invalid at the HTTP client level, which is fine
                    // The important thing is that the server doesn't crash with InvalidPathException
                }
            }
        }
    }
}