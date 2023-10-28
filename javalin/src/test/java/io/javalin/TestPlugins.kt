package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.PluginPriority.EARLY
import io.javalin.plugin.PluginPriority.LATE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.function.Consumer

class TestPlugins {

    lateinit var calls: MutableList<Calls>

    enum class Calls {
        INIT,
        START
    }

    open inner class TestPlugin : Plugin<Void>() {
        override fun onInitialize(config: JavalinConfig) {
            calls.add(Calls.INIT)
        }

        override fun onStart(config: JavalinConfig) {
            calls.add(Calls.START)
        }
    }

    @BeforeEach
    fun resetCalls() {
        calls = mutableListOf()
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
            Calls.START,
            Calls.START
        )
    }

    @Test
    fun `registerPlugin should work with anonymous objects`() {
        var called = false

        Javalin.create {
            it.registerPlugin(object : Plugin<Void>() {
                override fun onStart(config: JavalinConfig) {
                    called = true
                }
            })
        }

        assertThat(called).isTrue
    }

    @Test
    fun `registerPlugin should throw error if plugin is already registered`() {
        Javalin.create {
            it.registerPlugin(TestPlugin())

            assertThatThrownBy { it.registerPlugin(TestPlugin()) }
                .isInstanceOf(PluginAlreadyRegisteredException::class.java)
                .hasMessageContaining("TestPlugin is already registered")
        }
    }

    class MultiInstanceTestPlugin : Plugin<Void>() {
        override fun repeatable() = true
    }

    @Test
    fun `registerPlugin should not throw error for repeatable plugins`() {
        Javalin.create {
            assertDoesNotThrow {
                it.registerPlugin(MultiInstanceTestPlugin())
                it.registerPlugin(MultiInstanceTestPlugin())
            }
        }
    }

    @Test
    fun `plugins are initialized in the proper order`() {
        val calls = mutableListOf<String>()

        class EarlyPlugin : Plugin<Void>() {
            override fun priority() = EARLY
            override fun onInitialize(config: JavalinConfig) {
                calls.add("early-init")
            }

            override fun onStart(config: JavalinConfig) {
                calls.add("early-start")
            }
        }

        class NormalPlugin : Plugin<Void>() {
            override fun onInitialize(config: JavalinConfig) {
                calls.add("normal-init")
            }

            override fun onStart(config: JavalinConfig) {
                calls.add("normal-start")
            }
        }

        class LatePlugin : Plugin<Void>() {
            override fun priority() = LATE
            override fun onInitialize(config: JavalinConfig) {
                calls.add("late-init")
            }

            override fun onStart(config: JavalinConfig) {
                calls.add("late-start")
            }
        }

        Javalin.create { config ->
            config.registerPlugin(NormalPlugin())
            config.registerPlugin(LatePlugin())
            config.registerPlugin(EarlyPlugin())
        }

        assertThat(calls).containsExactly(
            "normal-init",
            "late-init",
            "early-init",
            "early-start",
            "normal-start",
            "late-start"
        )
    }

    @Test
    fun `plugin should be able to register a new plugin in init phase`() {
        val calls = mutableListOf<String>()

        class Plugin3 : Plugin<Void>() {
            override fun onInitialize(config: JavalinConfig) {
                calls.add("3")
            }
        }

        class Plugin2 : Plugin<Void>() {
            override fun onInitialize(config: JavalinConfig) {
                calls.add("2")
            }
        }

        class Plugin1 : Plugin<Void>() {
            override fun onInitialize(config: JavalinConfig) {
                calls.add("1")
                config.registerPlugin(Plugin2())
            }
        }

        Javalin.create { config ->
            config.registerPlugin(Plugin1())
            config.registerPlugin(Plugin3())
        }

        assertThat(calls).containsExactly("1", "2", "3")
    }

    @Test
    fun `registerPlugin returns the registered plugin`() {
        class PluginConfig(var value: String = "...")
        class MyPlugin(userConfig: Consumer<PluginConfig>) : Plugin<PluginConfig>(userConfig, PluginConfig()) {
            val value = pluginConfig.value
        }
        val myPlugin = JavalinConfig().registerPlugin(MyPlugin { it.value = "Hello" }) as MyPlugin
        assertThat(myPlugin.value).isEqualTo("Hello")
    }


    @Test
    fun `pluginConfig throws if defaultConfig is null`() {
        assertThatThrownBy { Javalin.create{ it.registerPlugin(ThrowingPlugin()) }.start() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Plugin io.javalin.ThrowingPlugin has no config.")
    }
}

class ThrowingPlugin : Plugin<Void>() {
    override fun onStart(config: JavalinConfig) {
        pluginConfig // should throw
    }
}
