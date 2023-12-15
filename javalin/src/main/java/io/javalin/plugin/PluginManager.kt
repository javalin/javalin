package io.javalin.plugin

import io.javalin.config.JavalinConfig

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

    fun <T> getContextPlugin(clazz: Class<out ContextPlugin<*, T>>): ContextPlugin<*, T> {
        return (plugins.find { it.javaClass == clazz } ?: throw PluginNotRegisteredException(clazz)) as ContextPlugin<*, T>
    }
}
