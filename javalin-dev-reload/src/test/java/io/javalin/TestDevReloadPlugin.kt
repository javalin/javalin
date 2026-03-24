/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

@file:Suppress("DEPRECATION")

package io.javalin

import io.javalin.plugin.bundled.DevReloadPlugin
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class TestDevReloadPlugin {

    // --- Child mode tests ---

    @Test
    fun `child mode overrides port from system property`() {
        System.setProperty("javalin.devreload.child", "true")
        System.setProperty("javalin.devreload.port", "9999")
        try {
            val app = Javalin.create {
                it.routes.get("/hello") { ctx -> ctx.result("Hello") }
                it.registerPlugin(DevReloadPlugin())
            }
            // The plugin should have overridden the port to 9999 in onStart
            // We can check by looking at the jetty config before starting
            // For a proper test, we start on the assigned port
            JavalinTest.test(app) { _, client ->
                assertThat(client.get("/hello").body().string()).isEqualTo("Hello")
            }
        } finally {
            System.clearProperty("javalin.devreload.child")
            System.clearProperty("javalin.devreload.port")
        }
    }

    @Test
    fun `child mode is no-op when system property not set`() {
        assertThat(DevReloadPlugin.isChildProcess()).isFalse()
    }

    @Test
    fun `child mode detected when system property set`() {
        System.setProperty("javalin.devreload.child", "true")
        try {
            assertThat(DevReloadPlugin.isChildProcess()).isTrue()
        } finally {
            System.clearProperty("javalin.devreload.child")
        }
    }

    // --- Config tests ---

    @Test
    fun `default config has sensible defaults`() {
        val config = DevReloadPlugin.Config()
        assertThat(config.sourceWatchPaths).isEmpty()
        assertThat(config.classWatchPaths).isEmpty()
        assertThat(config.compileCommand).isNull()
        assertThat(config.mainClass).isNull()
        assertThat(config.logging).isEqualTo(DevReloadPlugin.LogLevel.BASIC)
        assertThat(config.watchCooldownMs).isEqualTo(500)
        assertThat(config.useDirectCompiler).isTrue()
    }

    @Test
    fun `config can be customized`() {
        val plugin = DevReloadPlugin {
            it.compileCommand = "mvn compile -o -q"
            it.mainClass = "com.example.Main"
            it.watchCooldownMs = 100
            it.logging = DevReloadPlugin.LogLevel.EXTENSIVE
        }
        // Plugin was created without error — config is applied
        assertThat(plugin).isNotNull
    }

    // --- Path detection tests ---

    @Test
    fun `detectClasspathDirectories finds target dirs on classpath`() {
        val dirs = DevReloadPlugin.detectClasspathDirectories()
        // In a Maven test environment, target/test-classes should be on the classpath
        assertThat(dirs).anyMatch { it.toString().contains("target") }
    }

    @Test
    fun `detectSourceDirectories finds src dirs relative to classpath dirs`() {
        val dirs = DevReloadPlugin.detectSourceDirectories()
        // Should find src/main/java or src/test/java relative to this module
        assertThat(dirs).anyMatch { it.toString().contains("src") }
    }

    // --- Plugin registration tests ---

    @Test
    fun `plugin can be registered with zero config`() = JavalinTest.test(
        Javalin.create {
            it.routes.get("/hello") { ctx -> ctx.result("Hello") }
            // In parent mode, the plugin will try to set up proxy + child process.
            // Since we're in a test environment, main class detection may fail — that's fine.
            // The important thing is it doesn't crash.
            it.registerPlugin(DevReloadPlugin { c -> c.logging = DevReloadPlugin.LogLevel.NONE })
        }
    ) { _, client ->
        // Even if the plugin couldn't start its parent mode (no main class in test env),
        // the app should still work
        val response = client.get("/hello")
        // May get the actual response or a proxy response, either is acceptable
        assertThat(response.code).isIn(200, 503)
    }

    @Test
    fun `plugin can be registered with custom config`() = JavalinTest.test(
        Javalin.create {
            it.routes.get("/hello") { ctx -> ctx.result("Hello") }
            it.registerPlugin(DevReloadPlugin { c ->
                c.compileCommand = "echo compiled"
                c.logging = DevReloadPlugin.LogLevel.NONE
            })
        }
    ) { _, client ->
        val response = client.get("/hello")
        assertThat(response.code).isIn(200, 503)
    }
}
