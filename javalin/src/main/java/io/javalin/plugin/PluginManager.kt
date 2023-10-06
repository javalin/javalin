package io.javalin.plugin

import io.javalin.config.JavalinConfig

class PluginManager internal constructor(private val cfg: JavalinConfig) {

    private val plugins = mutableListOf<JavalinPlugin<*>>()
    private val initializedPlugins = mutableListOf<JavalinPlugin<*>>()
    private val enabledPlugins = mutableListOf<JavalinPlugin<*>>()

    fun register(plugin: JavalinPlugin<*>) {
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

}
