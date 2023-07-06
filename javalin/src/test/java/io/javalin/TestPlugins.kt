package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.PluginPriority.EARLY
import io.javalin.plugin.PluginPriority.LATE
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TestPlugins {

    lateinit var calls: MutableList<Calls>

    enum class Calls {
        INIT,
        START
    }

    open inner class TestPlugin : JavalinPlugin {
        override fun onInitialize(config: JavalinConfig) {
            calls.add(Calls.INIT)
        }
        override fun onStart(app: Javalin) {
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
            it.plugins.register(plugin1)
            it.plugins.register(plugin2)
        }

        assertThat(calls).containsExactly(
            Calls.INIT,
            Calls.INIT,
            Calls.START,
            Calls.START
        )
    }

    @Test
    fun `registerPlugin should work with lambdas`() {
        var called = false

        Javalin.create {
            it.plugins.register {
                called = true
            }
        }

        assertThat(called).isTrue
    }

    @Test
    fun `registerPlugin should throw error if plugin is already registered`() {
        Javalin.create {
            it.plugins.register(TestPlugin())

            assertThatThrownBy { it.plugins.register(TestPlugin()) }
                .isInstanceOf(PluginAlreadyRegisteredException::class.java)
                .hasMessage("TestPlugin is already registered")
        }
    }

    class MultiInstanceTestPlugin : JavalinPlugin {
        override fun onStart(app: Javalin) {}
        override fun repeatable() = true
    }

    @Test
    fun `registerPlugin should not throw error for repeatable plugins`() {
        Javalin.create {
            assertDoesNotThrow {
                it.plugins.register(MultiInstanceTestPlugin())
                it.plugins.register(MultiInstanceTestPlugin())
            }
        }
    }

    @Test
    fun `uninitialized plugins should be picked up in update config`() {
        var pluginAInitCount = 0
        var pluginBInitCount = 0

        TestUtil.test(Javalin.create { config ->
            config.plugins.register { pluginAInitCount++ }
        }) { app, _ ->
            app.updateConfig { config ->
                config.plugins.register { pluginBInitCount++ }
            }
        }

        assertThat(pluginAInitCount).isEqualTo(1) // make sure that plugin A was not initialized 2 times
        assertThat(pluginBInitCount).isEqualTo(1) // make sure that plugin B has been initialized
    }

    @Test
    fun `plugins are initialized in the proper order`() {
        val calls = mutableListOf<String>()

        class EarlyPlugin : JavalinPlugin {
            override fun priority() = EARLY
            override fun onInitialize(config: JavalinConfig) { calls.add("early-init") }
            override fun onStart(app: Javalin) { calls.add("early-start") }
        }

        class NormalPlugin : JavalinPlugin {
            override fun onInitialize(config: JavalinConfig) { calls.add("normal-init") }
            override fun onStart(app: Javalin) { calls.add("normal-start") }
        }

        class LatePlugin : JavalinPlugin {
            override fun priority() = LATE
            override fun onInitialize(config: JavalinConfig) { calls.add("late-init") }
            override fun onStart(app: Javalin) { calls.add("late-start") }
        }

        Javalin.create { config ->
            config.plugins.register(NormalPlugin())
            config.plugins.register(LatePlugin())
            config.plugins.register(EarlyPlugin())
        }

        assertThat(calls).containsExactly(
            "early-init",
            "normal-init",
            "late-init",
            "early-start",
            "normal-start",
            "late-start"
        )
    }

}
