/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

@file:Suppress("DEPRECATION")

package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.http.queryParamAsClass
import io.javalin.plugin.bundled.DevReloadPlugin
import io.javalin.testtools.JavalinTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class TestDevReloadPlugin {

    /** Creates a Javalin app with DevReloadPlugin pre-wired (onReload = no-op). */
    private fun devReloadApp(config: Consumer<JavalinConfig>) = Javalin.create { cfg ->
        config.accept(cfg)
        cfg.registerPlugin(DevReloadPlugin { it.onReload = Consumer {} })
    }

    private fun triggerReload(app: Javalin, reloadConfig: Consumer<JavalinConfig>) {
        findPlugin(app).reload(app.unsafe, reloadConfig)
    }

    @Suppress("UNCHECKED_CAST")
    private fun findPlugin(app: Javalin): DevReloadPlugin {
        val field = app.unsafe.pluginManager.javaClass.getDeclaredField("plugins")
        field.isAccessible = true
        return (field.get(app.unsafe.pluginManager) as List<*>).first { it is DevReloadPlugin } as DevReloadPlugin
    }

    @Test
    fun `routes are cleared and re-registered on reload`() = JavalinTest.test(
        devReloadApp { it.routes.get("/hello") { ctx -> ctx.result("Original") } }
    ) { app, client ->
        assertThat(client.get("/hello").body().string()).isEqualTo("Original")
        triggerReload(app, Consumer { it.routes.get("/hello") { ctx -> ctx.result("Reloaded") } })
        assertThat(client.get("/hello").body().string()).isEqualTo("Reloaded")
    }

    @Test
    fun `old routes return 404 after reload removes them`() = JavalinTest.test(
        devReloadApp {
            it.routes.get("/hello") { ctx -> ctx.result("Hello") }
            it.routes.get("/goodbye") { ctx -> ctx.result("Goodbye") }
        }
    ) { app, client ->
        assertThat(client.get("/hello").code).isEqualTo(200)
        assertThat(client.get("/goodbye").code).isEqualTo(200)
        triggerReload(app, Consumer { it.routes.get("/hello") { ctx -> ctx.result("Hello") } })
        assertThat(client.get("/hello").code).isEqualTo(200)
        assertThat(client.get("/goodbye").code).isEqualTo(404)
    }

    @Test
    fun `new routes are accessible after reload adds them`() = JavalinTest.test(
        devReloadApp { it.routes.get("/hello") { ctx -> ctx.result("Hello") } }
    ) { app, client ->
        assertThat(client.get("/new-route").code).isEqualTo(404)
        triggerReload(app, Consumer {
            it.routes.get("/hello") { ctx -> ctx.result("Hello") }
            it.routes.get("/new-route") { ctx -> ctx.result("I'm new!") }
        })
        assertThat(client.get("/new-route").code).isEqualTo(200)
        assertThat(client.get("/new-route").body().string()).isEqualTo("I'm new!")
    }

    @Test
    fun `exception handlers are reloaded`() = JavalinTest.test(
        devReloadApp {
            it.routes.get("/error") { throw IllegalStateException("boom") }
            it.routes.exception(IllegalStateException::class.java) { _, ctx -> ctx.status(500).result("Original handler") }
        }
    ) { app, client ->
        assertThat(client.get("/error").body().string()).isEqualTo("Original handler")
        triggerReload(app, Consumer {
            it.routes.get("/error") { throw IllegalStateException("boom") }
            it.routes.exception(IllegalStateException::class.java) { _, ctx -> ctx.status(500).result("Reloaded handler") }
        })
        assertThat(client.get("/error").body().string()).isEqualTo("Reloaded handler")
    }

    @Test
    fun `error handlers are reloaded`() = JavalinTest.test(
        devReloadApp { it.routes.error(404) { ctx -> ctx.result("Original 404") } }
    ) { app, client ->
        assertThat(client.get("/nonexistent").body().string()).isEqualTo("Original 404")
        triggerReload(app, Consumer { it.routes.error(404) { ctx -> ctx.result("Reloaded 404") } })
        assertThat(client.get("/nonexistent").body().string()).isEqualTo("Reloaded 404")
    }

    @Test
    fun `server stays running on same port through reload`() = JavalinTest.test(
        devReloadApp { it.routes.get("/hello") { ctx -> ctx.result("Hello") } }
    ) { app, client ->
        val portBefore = app.port()
        triggerReload(app, Consumer { it.routes.get("/hello") { ctx -> ctx.result("Hello after reload") } })
        assertThat(app.port()).isEqualTo(portBefore)
        assertThat(app.jettyServer().server().isRunning).isTrue()
        assertThat(client.get("/hello").body().string()).isEqualTo("Hello after reload")
    }

    @Test
    fun `multiple reloads work correctly`() = JavalinTest.test(
        devReloadApp { it.routes.get("/counter") { ctx -> ctx.result("v1") } }
    ) { app, client ->
        assertThat(client.get("/counter").body().string()).isEqualTo("v1")
        triggerReload(app, Consumer { it.routes.get("/counter") { ctx -> ctx.result("v2") } })
        assertThat(client.get("/counter").body().string()).isEqualTo("v2")
        triggerReload(app, Consumer { it.routes.get("/counter") { ctx -> ctx.result("v3") } })
        assertThat(client.get("/counter").body().string()).isEqualTo("v3")
    }

    @Test
    fun `reload is resilient to errors in config consumer`() = JavalinTest.test(
        devReloadApp { it.routes.get("/hello") { ctx -> ctx.result("Hello") } }
    ) { app, client ->
        assertThat(client.get("/hello").body().string()).isEqualTo("Hello")
        triggerReload(app, Consumer { throw RuntimeException("Config error!") })
        assertThat(app.jettyServer().server().isRunning).isTrue()
    }

    @Test
    fun `validation exception mapper is re-added after reload`() = JavalinTest.test(
        devReloadApp { it.routes.get("/validate") { ctx -> ctx.queryParamAsClass<Int>("age").get(); ctx.result("OK") } }
    ) { app, client ->
        assertThat(client.get("/validate?age=notanumber").code).isEqualTo(400)
        triggerReload(app, Consumer { it.routes.get("/validate") { ctx -> ctx.queryParamAsClass<Int>("age").get(); ctx.result("OK") } })
        assertThat(client.get("/validate?age=notanumber").code).isEqualTo(400)
    }

    @Test
    fun `zero-config registerPlugin auto-detects create config`() = JavalinTest.test(
        Javalin.create { it.routes.get("/hello") { ctx -> ctx.result("Auto") }; it.registerPlugin(DevReloadPlugin()) }
    ) { app, client ->
        assertThat(client.get("/hello").body().string()).isEqualTo("Auto")
        findPlugin(app).reload(app.unsafe, Consumer { it.routes.get("/hello") { ctx -> ctx.result("Auto") } })
        assertThat(client.get("/hello").body().string()).isEqualTo("Auto")
        assertThat(app.jettyServer().server().isRunning).isTrue()
    }

    // --- End-to-end tests: file change → automatic reload on next request ---

    private val controllerFile = Path.of("src/test/java/devreloadtest/DevReloadController.java")
    private val originalContent = controllerFile.let { Files.readString(it) }

    private fun withModifiedController(result: String, block: () -> Unit) {
        try {
            Files.writeString(controllerFile, originalContent.replace(""""original"""", """"$result""""))
            block()
        } finally {
            Files.writeString(controllerFile, originalContent)
        }
    }

    @Test
    fun `source file change triggers compile and reload on next request`() = withModifiedController("modified") {
        JavalinTest.test(
            Javalin.create {
                it.routes.get("/test") { ctx -> devreloadtest.DevReloadController.handle(ctx) }
                it.registerPlugin(DevReloadPlugin { it.watchCooldownMs = 0 })
            }
        ) { _, client ->
            assertThat(client.get("/test").body().string()).isEqualTo("original")
            Files.writeString(controllerFile, originalContent.replace(""""original"""", """"modified""""))
            Thread.sleep(50)
            assertThat(client.get("/test").body().string()).isEqualTo("modified")
        }
    }

    @Test
    fun `unchanged files do not trigger reload`(@TempDir tempDir: Path) {
        val classFile = tempDir.resolve("Dummy.class")
        Files.writeString(classFile, "v1")
        val reloadCount = AtomicInteger(0)
        JavalinTest.test(
            Javalin.create {
                it.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") }
                it.registerPlugin(DevReloadPlugin { reload ->
                    reload.classWatchPaths = listOf(tempDir)
                    reload.onReload = Consumer { cfg -> reloadCount.incrementAndGet(); cfg.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") } }
                })
            }
        ) { _, client ->
            repeat(3) { client.get("/count").body().string() }
            assertThat(reloadCount.get()).isEqualTo(0)
        }
    }
}
