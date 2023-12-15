package io.javalin.plugin

import io.javalin.config.JavalinConfig
import io.javalin.util.parentClass
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class PluginManager internal constructor(private val cfg: JavalinConfig) {

    private val plugins = mutableListOf<Plugin<*>>()
    private val initializedPlugins = mutableListOf<Plugin<*>>()
    private val enabledPlugins = mutableListOf<Plugin<*>>()

    fun register(plugin: Plugin<*>) {
        if (!plugin.repeatable() && plugins.any { it.javaClass == plugin.javaClass }) {
            throw PluginAlreadyRegisteredException(plugin)
        }
        plugins.add(plugin)
        initializePlugins()
    }

    private fun initializePlugins() {
        while (plugins.size != initializedPlugins.size) {
            val pluginsToInitialize = plugins
                .asSequence()
                .filter { it !in initializedPlugins }
                .sortedBy { it.priority() }

            for (plugin in pluginsToInitialize) {
                initializedPlugins.add(plugin)
                plugin.onInitialize(cfg)
            }
        }
    }

    fun startPlugins() {
        initializedPlugins
            .asSequence()
            .filter { it !in enabledPlugins }
            .sortedBy { it.priority() }
            .forEach {
                it.onStart(cfg)
                enabledPlugins.add(it)
            }
    }

    fun <T> getContextPlugin(clazz: Class<out ContextExtendingPlugin<*, T>>): ContextExtendingPlugin<*, T> {
        return (plugins.find { it.javaClass == clazz } ?: throw PluginNotRegisteredException(clazz)) as ContextExtendingPlugin<*, T>
    }
}
