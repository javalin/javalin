package io.javalin

import io.javalin.core.getPlugin
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginAlreadyRegisteredException
import io.javalin.core.plugin.PluginInitLifecycleViolationException
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.plugin.PluginNotFoundException
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test

lateinit var calls: MutableList<Calls>

enum class Calls {
    INIT,
    APPLY
}

open class TestPlugin : Plugin, PluginLifecycleInit {
    override fun init(app: Javalin) {
        calls.add(Calls.INIT)
    }

    override fun apply(app: Javalin) {
        calls.add(Calls.APPLY)
    }
}

private interface PluginWithAllLifecycles : Plugin, PluginLifecycleInit

class TestPlugins {
    @Before
    fun resetCalls() {
        calls = mutableListOf()
    }

    @Test
    fun `should run lifecycle methods with right arguments`() {
        val plugin = spyk<PluginWithAllLifecycles>()

        val app = Javalin.create {
            it.registerPlugin(plugin)
        }

        verify { plugin.init(app) }
        verify { plugin.apply(app) }
    }

    @Test
    fun `should run lifecycle methods in right order`() {
        class TestPlugin1 : TestPlugin()
        class TestPlugin2 : TestPlugin()

        val plugin1 = TestPlugin1()
        val plugin2 = TestPlugin2()

        Javalin.create {
            it.registerPlugin(plugin1)
            it.registerPlugin(plugin2)
        }

        assertThat(calls).containsExactly(
                Calls.INIT,
                Calls.INIT,
                Calls.APPLY,
                Calls.APPLY
        )
    }

    @Test
    fun `registerPlugin should work with lambdas`() {
        var called = false

        Javalin.create {
            it.registerPlugin {
                called = true
            }
        }

        assertThat(called).isTrue()
    }

    @Test
    fun `registerPlugin should throw error if plugin is already registered`() {
        Javalin.create {
            it.registerPlugin(TestPlugin())

            assertThatThrownBy {
                it.registerPlugin(TestPlugin())
            }.isEqualTo(PluginAlreadyRegisteredException(TestPlugin::class.java))
        }
    }

    @Test
    fun `init should throw error if handler is registered in init`() {
        class BadPlugin : Plugin, PluginLifecycleInit {
            override fun apply(app: Javalin) {}

            override fun init(app: Javalin) {
                app.get("/hello") {}
            }

        }

        assertThatThrownBy {
            Javalin.create { it.registerPlugin(BadPlugin()) }
        }.isEqualTo(PluginInitLifecycleViolationException(BadPlugin::class.java))
    }

    @Test
    fun `getPlugin should return instance`() {
        val plugin = TestPlugin()

        val app = Javalin.create {
            it.registerPlugin(plugin)
        }

        assertThat(app.config.getPlugin<TestPlugin>()).isEqualTo(plugin)
    }

    @Test
    fun `getPlugin should throw an error if instance does not exists`() {
        val app = Javalin.create {}

        assertThatThrownBy {
            app.config.getPlugin<TestPlugin>()
        }.isEqualTo(PluginNotFoundException(TestPlugin::class.java))
    }
}
