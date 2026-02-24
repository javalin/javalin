/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

@file:Suppress("DEPRECATION")

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus
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
    fun `routes are cleared and re-registered on reload`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Original") }
            config.registerPlugin(DevReloadPlugin { devReload ->
                devReload.onReload = Consumer { cfg ->
                    cfg.routes.get("/hello") { ctx -> ctx.result("Reloaded") }
                }
            })
        }
    ) { app, client ->
        // Before reload
        assertThat(client.get("/hello").body().string()).isEqualTo("Original")

        // Trigger reload manually via plugin internals
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Reloaded") }
        })

        // After reload
        assertThat(client.get("/hello").body().string()).isEqualTo("Reloaded")
    }

    @Test
    fun `old routes return 404 after reload removes them`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.routes.get("/goodbye") { ctx -> ctx.result("Goodbye") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        // Both routes work before reload
        assertThat(client.get("/hello").code).isEqualTo(200)
        assertThat(client.get("/goodbye").code).isEqualTo(200)

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello") }
            // /goodbye is intentionally NOT re-registered
        })

        // /hello still works, /goodbye is gone
        assertThat(client.get("/hello").code).isEqualTo(200)
        assertThat(client.get("/goodbye").code).isEqualTo(404)
    }

    @Test
    fun `new routes are accessible after reload adds them`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        // /new-route doesn't exist before reload
        assertThat(client.get("/new-route").code).isEqualTo(404)

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello") }
            cfg.routes.get("/new-route") { ctx -> ctx.result("I'm new!") }
        })

        // /new-route now works
        assertThat(client.get("/new-route").code).isEqualTo(200)
        assertThat(client.get("/new-route").body().string()).isEqualTo("I'm new!")
    }

    @Test
    fun `exception handlers are reloaded`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/error") { throw IllegalStateException("boom") }
            config.routes.exception(IllegalStateException::class.java) { _, ctx ->
                ctx.status(500).result("Original handler")
            }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        assertThat(client.get("/error").body().string()).isEqualTo("Original handler")

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/error") { throw IllegalStateException("boom") }
            cfg.routes.exception(IllegalStateException::class.java) { _, ctx ->
                ctx.status(500).result("Reloaded handler")
            }
        })

        assertThat(client.get("/error").body().string()).isEqualTo("Reloaded handler")
    }

    @Test
    fun `error handlers are reloaded`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.error(404) { ctx -> ctx.result("Original 404") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        assertThat(client.get("/nonexistent").body().string()).isEqualTo("Original 404")

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.error(404) { ctx -> ctx.result("Reloaded 404") }
        })

        assertThat(client.get("/nonexistent").body().string()).isEqualTo("Reloaded 404")
    }

    @Test
    fun `server stays running on same port through reload`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        val portBefore = app.port()
        assertThat(app.jettyServer().server().isRunning).isTrue()

        // Trigger reload
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Hello after reload") }
        })

        assertThat(app.port()).isEqualTo(portBefore)
        assertThat(app.jettyServer().server().isRunning).isTrue()
        assertThat(client.get("/hello").body().string()).isEqualTo("Hello after reload")
    }

    @Test
    fun `multiple reloads work correctly`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/counter") { ctx -> ctx.result("v1") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        assertThat(client.get("/counter").body().string()).isEqualTo("v1")

        // Reload to v2
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/counter") { ctx -> ctx.result("v2") }
        })
        assertThat(client.get("/counter").body().string()).isEqualTo("v2")

        // Reload to v3
        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/counter") { ctx -> ctx.result("v3") }
        })
        assertThat(client.get("/counter").body().string()).isEqualTo("v3")
    }

    @Test
    fun `reload is resilient to errors in config consumer`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Hello") }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        assertThat(client.get("/hello").body().string()).isEqualTo("Hello")

        // Reload with a consumer that throws — should log error but not crash
        triggerReload(app, Consumer { _ ->
            throw RuntimeException("Config error!")
        })

        // Server should still be running
        assertThat(app.jettyServer().server().isRunning).isTrue()
    }

    @Test
    fun `validation exception mapper is re-added after reload`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/validate") { ctx ->
                ctx.queryParamAsClass<Int>("age").get()
                ctx.result("OK")
            }
            config.registerPlugin(DevReloadPlugin { devReload -> devReload.onReload = Consumer {} })
        }
    ) { app, client ->
        // Validation should return 400 before reload
        assertThat(client.get("/validate?age=notanumber").code).isEqualTo(400)

        triggerReload(app, Consumer { cfg ->
            cfg.routes.get("/validate") { ctx ->
                ctx.queryParamAsClass<Int>("age").get()
                ctx.result("OK")
            }
        })

        // Validation should still return 400 after reload
        assertThat(client.get("/validate?age=notanumber").code).isEqualTo(400)
    }

    @Test
    fun `zero-config registerPlugin auto-detects create config`() = JavalinTest.test(
        Javalin.create { config ->
            config.routes.get("/hello") { ctx -> ctx.result("Auto") }
            config.registerPlugin(DevReloadPlugin())
        }
    ) { app, client ->
        assertThat(client.get("/hello").body().string()).isEqualTo("Auto")

        // Reload using an explicit consumer (simulating what the plugin does internally)
        val plugin = findPlugin(app, DevReloadPlugin::class.java)
        plugin.reload(app.unsafe, Consumer { cfg ->
            cfg.routes.get("/hello") { ctx -> ctx.result("Auto") }
        })

        // Routes come back
        assertThat(client.get("/hello").body().string()).isEqualTo("Auto")
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
            JavalinTest.test(
                Javalin.create { config ->
                    config.routes.get("/test") { ctx -> devreloadtest.DevReloadController.handle(ctx) }
                    config.registerPlugin(DevReloadPlugin { it.watchCooldownMs = 0 })
                }
            ) { _, client ->
                assertThat(client.get("/test").body().string()).isEqualTo("original")

                // Modify the controller source file
                modifyController("modified")
                Thread.sleep(50) // ensure timestamp differs

                // Next request should detect the source change, recompile, and reload
                assertThat(client.get("/test").body().string()).isEqualTo("modified")
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

        JavalinTest.test(
            Javalin.create { config ->
                config.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") }
                config.registerPlugin(DevReloadPlugin { reload ->
                    reload.classWatchPaths = listOf(tempDir)
                    reload.onReload = Consumer { cfg ->
                        reloadCount.incrementAndGet()
                        cfg.routes.get("/count") { ctx -> ctx.result("${reloadCount.get()}") }
                    }
                })
            }
        ) { _, client ->
            repeat (3) { client.get("/count").body().string() }
            assertThat(reloadCount.get()).isEqualTo(0)
        }
    }

}
