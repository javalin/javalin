package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.PluginPriority.EARLY
import io.javalin.plugin.PluginPriority.LATE
import java.util.function.Consumer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class TestPlugins {

    enum class Calls {
        INIT,
        START
    }

    open class LifecycleTestPlugin(private val calls: MutableList<Calls>) : JavalinPlugin<Unit>(LifecycleTestPlugin, Unit) {
        companion object : PluginFactory<LifecycleTestPlugin, Unit> {
            override fun create(config: Consumer<Unit>): LifecycleTestPlugin = LifecycleTestPlugin(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) {
            calls.add(Calls.INIT)
        }
        override fun onStart(config: JavalinConfig) {
            calls.add(Calls.START)
        }
    }

    @Test
    fun `should run lifecycle methods in right order`() {
        val calls = mutableListOf<Calls>()
        class TestPlugin1 : LifecycleTestPlugin(calls)
        class TestPlugin2 : LifecycleTestPlugin(calls)

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
    fun `registerPlugin should throw error if plugin is already registered`() {
        Javalin.create {
            it.registerPlugin(LifecycleTestPlugin(mutableListOf()))

            assertThatThrownBy { it.registerPlugin(LifecycleTestPlugin(mutableListOf())) }
                .isInstanceOf(PluginAlreadyRegisteredException::class.java)
                .hasMessage("LifecycleTestPlugin is already registered")
        }
    }

    class MultiInstanceTestPlugin : JavalinPlugin<Unit>(MultiInstanceTestPlugin, Unit, repeatable = true) {
        companion object : PluginFactory<MultiInstanceTestPlugin, Unit> {
            override fun create(config: Consumer<Unit>): MultiInstanceTestPlugin = MultiInstanceTestPlugin()
        }
        override fun onStart(config: JavalinConfig) {}
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

    class EarlyPlugin(private val calls: MutableList<String>) : JavalinPlugin<Unit>(EarlyPlugin, Unit, priority = EARLY) {
        companion object : PluginFactory<EarlyPlugin, Unit> {
            override fun create(config: Consumer<Unit>): EarlyPlugin = EarlyPlugin(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) { calls.add("early-init") }
        override fun onStart(config: JavalinConfig) { calls.add("early-start") }
    }
    class NormalPlugin(private val calls: MutableList<String>) : JavalinPlugin<Unit>(NormalPlugin, Unit) {
        companion object : PluginFactory<NormalPlugin, Unit> {
            override fun create(config: Consumer<Unit>): NormalPlugin = NormalPlugin(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) { calls.add("normal-init") }
        override fun onStart(config: JavalinConfig) { calls.add("normal-start") }
    }
    class LatePlugin(private val calls: MutableList<String>) : JavalinPlugin<Unit>(LatePlugin, Unit, priority = LATE) {
        companion object : PluginFactory<LatePlugin, Unit> {
            override fun create(config: Consumer<Unit>): LatePlugin = LatePlugin(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) { calls.add("late-init") }
        override fun onStart(config: JavalinConfig) { calls.add("late-start") }
    }

    @Test
    fun `plugins are initialized in the proper order`() {
        val calls = mutableListOf<String>()

        Javalin.create { config ->
            config.registerPlugin(NormalPlugin(calls))
            config.registerPlugin(LatePlugin(calls))
            config.registerPlugin(EarlyPlugin(calls))
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

    class Plugin3(private val calls: MutableList<String>) : JavalinPlugin<Unit>(Plugin3, Unit) {
        companion object : PluginFactory<Plugin3, Unit> {
            override fun create(config: Consumer<Unit>): Plugin3 = Plugin3(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) { calls.add("3") }
        override fun onStart(config: JavalinConfig) {}
    }
    class Plugin2(private val calls: MutableList<String>) : JavalinPlugin<Unit>(Plugin2, Unit) {
        companion object : PluginFactory<Plugin2, Unit> {
            override fun create(config: Consumer<Unit>): Plugin2 = Plugin2(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) { calls.add("2") }
        override fun onStart(config: JavalinConfig) {}
    }
    class Plugin1(private val calls: MutableList<String>) : JavalinPlugin<Unit>(Plugin1, Unit) {
        companion object : PluginFactory<Plugin1, Unit> {
            override fun create(config: Consumer<Unit>): Plugin1 = Plugin1(mutableListOf())
        }
        override fun onInitialize(config: JavalinConfig) {
            calls.add("1")
            config.registerPlugin(Plugin2(calls))
        }
        override fun onStart(config: JavalinConfig) {}
    }

    @Test
    fun `plugin should be able to register a new plugin in init phase`() {
        val calls = mutableListOf<String>()

        Javalin.create { config ->
            config.registerPlugin(Plugin1(calls))
            config.registerPlugin(Plugin3(calls))
        }

        assertThat(calls).containsExactly("1", "2", "3")
    }

}
