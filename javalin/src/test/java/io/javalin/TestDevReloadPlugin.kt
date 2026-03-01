/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus
import io.javalin.http.queryParamAsClass
import io.javalin.plugin.bundled.DevReloadPlugin
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

class TestDevReloadPlugin {

    private fun triggerReload(app: Javalin, reloadConfig: Consumer<JavalinConfig>) {
        val plugin = findPlugin(app, DevReloadPlugin::class.java)
        plugin.reload(app.unsafe, reloadConfig)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findPlugin(app: Javalin, clazz: Class<T>): T {
        val pluginsField = app.unsafe.pluginManager.javaClass.getDeclaredField("plugins")
        pluginsField.isAccessible = true
        val plugins = pluginsField.get(app.unsafe.pluginManager) as List<*>
        return plugins.first { clazz.isAssignableFrom(it!!.javaClass) } as T
    }

    @Test
    fun `routes are cleared and re-registered on reload`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Original") }
            config.bundledPlugins.enableDevReload { devReload ->
                devReload.onReload = Consumer { cfg ->
                    cfg.routes.get("/hello") { ctx -> ctx.result("Reloaded") }
                }
            }
        }
    ) { app, http ->
        // Before reload
        assertThat(http.getBody("/hello")).isEqualTo("Original")

        // Trigger reload manually via plugin internals
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Reloaded") }
        })

        // After reload
        assertThat(http.getBody("/hello")).isEqualTo("Reloaded")
    }

    @Test
    fun `old routes return 404 after reload removes them`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.routes.get("/goodbye") { ctx -> ctx.result("Goodbye") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        // Both routes work before reload
        assertThat(http.get("/hello").httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(http.get("/goodbye").httpCode()).isEqualTo(HttpStatus.OK)

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello") }
            // /goodbye is intentionally NOT re-registered
        })

        // /hello still works, /goodbye is gone
        assertThat(http.get("/hello").httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(http.get("/goodbye").httpCode()).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `new routes are accessible after reload adds them`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        // /new-route doesn't exist before reload
        assertThat(http.get("/new-route").httpCode()).isEqualTo(HttpStatus.NOT_FOUND)

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello") }
            cfg.routes.get("/new-route") { ctx -> ctx.result("I'm new!") }
        })

        // /new-route now works
        assertThat(http.get("/new-route").httpCode()).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/new-route")).isEqualTo("I'm new!")
    }

    @Test
    fun `exception handlers are reloaded`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/error") { throw IllegalStateException("boom") }
            config.routes.exception(IllegalStateException::class.java) { _, ctx ->
                ctx.status(500).result("Original handler")
            }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        assertThat(http.getBody("/error")).isEqualTo("Original handler")

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/error") { throw IllegalStateException("boom") }
            cfg.routes.exception(IllegalStateException::class.java) { _, ctx ->
                ctx.status(500).result("Reloaded handler")
            }
        })

        assertThat(http.getBody("/error")).isEqualTo("Reloaded handler")
    }

    @Test
    fun `error handlers are reloaded`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.error(404) { ctx -> ctx.result("Original 404") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        assertThat(http.getBody("/nonexistent")).isEqualTo("Original 404")

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.error(404) { ctx -> ctx.result("Reloaded 404") }
        })

        assertThat(http.getBody("/nonexistent")).isEqualTo("Reloaded 404")
    }

    @Test
    fun `server stays running on same port through reload`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        val portBefore = app.port()
        assertThat(app.jettyServer().server().isRunning).isTrue()

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello after reload") }
        })

        assertThat(app.port()).isEqualTo(portBefore)
        assertThat(app.jettyServer().server().isRunning).isTrue()
        assertThat(http.getBody("/hello")).isEqualTo("Hello after reload")
    }

    @Test
    fun `multiple reloads work correctly`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/counter") { ctx -> ctx.result("v1") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        assertThat(http.getBody("/counter")).isEqualTo("v1")

        // Reload to v2
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/counter") { ctx -> ctx.result("v2") }
        })
        assertThat(http.getBody("/counter")).isEqualTo("v2")

        // Reload to v3
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/counter") { ctx -> ctx.result("v3") }
        })
        assertThat(http.getBody("/counter")).isEqualTo("v3")
    }

    @Test
    fun `reload is resilient to errors in config consumer`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        assertThat(http.getBody("/hello")).isEqualTo("Hello")

        // Reload with a consumer that throws — should log error but not crash
        triggerReload(app, Consumer { _ ->
            throw RuntimeException("Config error!")
        })

        // Server should still be running
        assertThat(app.jettyServer().server().isRunning).isTrue()
    }

    @Test
    fun `validation exception mapper is re-added after reload`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/validate") { ctx ->
                ctx.queryParamAsClass<Int>("age").get()
                ctx.result("OK")
            }
            config.bundledPlugins.enableDevReload { devReload -> devReload.onReload = Consumer {} }
        }
    ) { app, http ->
        // Validation should return 400 before reload
        assertThat(http.get("/validate?age=notanumber").httpCode()).isEqualTo(HttpStatus.BAD_REQUEST)

        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/validate") { ctx ->
                ctx.queryParamAsClass<Int>("age").get()
                ctx.result("OK")
            }
        })

        // Validation should still return 400 after reload
        assertThat(http.get("/validate?age=notanumber").httpCode()).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    @Test
    fun `zero-config enableDevReload auto-detects create config`() = TestUtil.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Auto") }
            config.bundledPlugins.enableDevReload()
        }
    ) { app, http ->
        assertThat(http.getBody("/hello")).isEqualTo("Auto")

        // Reload using the stored userConfig (same as original create consumer)
        val plugin = findPlugin(app, DevReloadPlugin::class.java)
        val storedConfig = app.unsafe.userConfig!!
        plugin.reload(app.unsafe, storedConfig)

        // The original create consumer is re-executed, so routes come back
        assertThat(http.getBody("/hello")).isEqualTo("Auto")
        assertThat(app.jettyServer().server().isRunning).isTrue()
    }

    // --- End-to-end tests: file change → automatic reload on next request ---

    private val controllerFile = Path.of("src/test/java/devreloadtest/DevReloadController.java")
    private val originalContent = controllerFile.let { Files.readString(it) }

    private fun modifyController(result: String) {
        val modified = originalContent.replace(""""original"""", """"$result"""")
        Files.writeString(controllerFile, modified)
    }

    private fun restoreController() {
        Files.writeString(controllerFile, originalContent)
    }

    @Test
    fun `source file change triggers compile and reload on next request`() {
        try {
            TestUtil.test(
                Javalin.create { config ->
                    config.routes.get("/test") { ctx -> devreloadtest.DevReloadController.handle(ctx) }
                    config.bundledPlugins.enableDevReload()
                }
            ) { _, http ->
                assertThat(http.getBody("/test")).isEqualTo("original")

                // Modify the controller source file
                modifyController("modified")
                Thread.sleep(50) // ensure timestamp differs

                // Next request should detect the source change, recompile, and reload
                assertThat(http.getBody("/test")).isEqualTo("modified")
            }
        } finally {
            restoreController()
        }
    }

    @Test
    fun `unchanged files do not trigger reload`(@TempDir tempDir: Path) {
        val classFile = tempDir.resolve("Dummy.class")
        Files.writeString(classFile, "v1")

        val reloadCount = AtomicInteger(0)

        TestUtil.test(
            Javalin.create { config ->
                config.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") }
                config.bundledPlugins.enableDevReload { reload ->
                    reload.classWatchPaths = listOf(tempDir)
                    reload.onReload = Consumer { cfg ->
                        reloadCount.incrementAndGet()
                        cfg.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") }
                    }
                }
            }
        ) { _, http ->
            repeat (3) { http.getBody("/count") }
            assertThat(reloadCount.get()).isEqualTo(0)
        }
    }

}
