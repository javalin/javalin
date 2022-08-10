package io.javalin

import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginAlreadyRegisteredException
import io.javalin.plugin.PluginInitException
import io.javalin.plugin.PluginLifecycleInit
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

class TestPlugins {

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
            Calls.APPLY,
            Calls.APPLY
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

        assertThat(called).isTrue()
    }

    @Test
    fun `registerPlugin should throw error if plugin is already registered`() {
        Javalin.create {
            it.plugins.register(TestPlugin())

            assertThatThrownBy {
                it.plugins.register(TestPlugin())
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
            Javalin.create { it.plugins.register(BadPlugin()) }
        }.isEqualTo(PluginInitException(BadPlugin::class.java))
    }

}
