package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.config.JavalinConfig

class PluginManager internal constructor() {

    private val plugins: MutableList<JavalinPlugin> = mutableListOf()
    private val enabledPlugins: MutableSet<JavalinPlugin> = mutableSetOf()

    fun register(plugin: JavalinPlugin) {
        if (!plugin.repeatable() && plugins.any { it.javaClass == plugin.javaClass }) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        plugins.add(plugin)
    }

    fun initializePlugins(app: Javalin, cfg: JavalinConfig) {
        val initializedPlugins = enabledPlugins.toMutableSet()

        while (plugins.size != initializedPlugins.size) {
            val amountOfPlugins = plugins.size

            val pluginsToInitialize = plugins
                .filterNot { enabledPlugins.contains(it) }
                .filterNot { initializedPlugins.contains(it) }
                .sortedBy { it.priority() }

            for (plugin in pluginsToInitialize) {
                plugin.onInitialize(cfg)
                initializedPlugins.add(plugin)

                if (amountOfPlugins != plugins.size) {
                    continue // plugin was registered during onInitialize, so we need to re-sort
                }
            }
        }

        initializedPlugins
            .filterNot { enabledPlugins.contains(it) }
            .sortedBy { it.priority() }
            .forEach {
                it.onStart(app)
                enabledPlugins.add(it)
            }
    }

}
