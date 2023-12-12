package io.javalin

import io.javalin.config.JavalinConfig
import io.javalin.plugin.ContextPlugin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.PluginNotFoundException
import io.javalin.plugin.PluginPriority.EARLY
import io.javalin.plugin.PluginPriority.LATE
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.concurrent.CountDownLatch
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
        class ThrowingPlugin : Plugin<Void>() {
            override fun onStart(config: JavalinConfig) {
                pluginConfig // should throw
            }
        }
        assertThatThrownBy { Javalin.create { it.registerPlugin(ThrowingPlugin()) }.start() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ThrowingPlugin has no config.")
    }

    @Test
    fun `Context-extending plugins can be accessed through the Context by class`() {
        class TestContextPlugin : ContextPlugin<Void>() {
            fun fancyPath(): String {
                return context().path() + "_FANCY"
            }
        }
        TestUtil.test(Javalin.create {
            it.registerPlugin(TestContextPlugin())
        }) { app, http ->
            app.get("/abcd") { it.result(it.with(TestContextPlugin::class.java).fancyPath()) }
            assertThat(http.getBody("/abcd")).isEqualTo("/abcd_FANCY")
        }
    }

    @Test
    fun `Context-extending plugins throw if they are not registered`() {
        class UnregisteredPlugin : ContextPlugin<Void>()
        TestUtil.test(Javalin.create {}) { app, http ->
            app.exception(PluginNotFoundException::class.java) { e, ctx -> ctx.result(e.message!!) }
            app.get("/") { it.with(UnregisteredPlugin::class.java) }
            assertThat(http.getBody("/")).contains("UnregisteredPlugin' not found")
        }
    }

    @Test
    fun `Context-extending plugins have thread-local context`() {
        class ThreadLocalContextPlugin : ContextPlugin<Void>() {
            fun value() = context().attribute<String>("value")!!
        }
        TestUtil.test(Javalin.create {
            it.registerPlugin(ThreadLocalContextPlugin())
        }) { app, http ->
            val pluginLatch = CountDownLatch(1)
            app.get("/") { ctx ->
                val value = ctx.queryParam("value")
                ctx.attribute("value", value) // set value for the plugin
                when (value) {
                    "Request1" -> pluginLatch.await() // Request1 must wait for Request2
                    else -> pluginLatch.countDown() // let Request1 continue
                }
                val pluginValue = ctx.with(ThreadLocalContextPlugin::class.java).value()
                ctx.result(pluginValue)
            }
            val requestLatch = CountDownLatch(2)
            val request1 = http.getAsync("/?value=Request1").also { it.thenAccept { requestLatch.countDown() } }
            val request2 = http.getAsync("/?value=Request2").also { it.thenAccept { requestLatch.countDown() } }
            requestLatch.await()
            assertThat(request1.get().body).isEqualTo("Request1") // without thread-local context, this would be "Request2"
            assertThat(request2.get().body).isEqualTo("Request2")
        }
    }

    @Test
    fun `Context-extending plugins can be used to create a custom renderer`() {
        class TplConfig(var directory: String = "...")
        class Tpl(userConfig : Consumer<TplConfig>) : ContextPlugin<TplConfig>(userConfig, TplConfig()) {
            fun render(path: String) {
                context().html(pluginConfig.directory + path)
            }
        }
        TestUtil.test(Javalin.create {
            it.registerPlugin(Tpl {
                it.directory = "src/test/resources"
            })
        }) { app, http ->
            app.get("/") { it.with(Tpl::class).render("/template.tpl") }
            assertThat(http.getBody("/")).isEqualTo("src/test/resources/template.tpl")
        }
    }

}
