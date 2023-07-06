package io.javalin.plugin

import io.javalin.Javalin

class PluginManager internal constructor() {

    private val plugins: MutableList<JavalinPlugin> = mutableListOf()
    private val enabledPlugins: MutableSet<JavalinPlugin> = mutableSetOf()

    fun register(plugin: JavalinPlugin) {
        if (!plugin.repeatable() && plugins.any { it.javaClass == plugin.javaClass }) {
            throw PluginAlreadyRegisteredException(plugin)
        }
        plugins.add(plugin)
    }

    fun initializePlugins(app: Javalin) {
        val initializedPlugins = enabledPlugins.toMutableSet()

        while (plugins.size != initializedPlugins.size) {
            val amountOfPlugins = plugins.size

            val pluginsToInitialize = plugins
                .asSequence()
                .filter { it !in enabledPlugins }
                .filter { it !in initializedPlugins }
                .sortedBy { it.priority() }

            for (plugin in pluginsToInitialize) {
                plugin.onInitialize(app.cfg)
                initializedPlugins.add(plugin)

                if (amountOfPlugins != plugins.size) {
                    continue // plugin was registered during onInitialize, so we need to re-sort
                }
            }
        }

        initializedPlugins
            .asSequence()
            .filter { it !in enabledPlugins }
            .sortedBy { it.priority() }
            .forEach {
                it.onStart(app)
                enabledPlugins.add(it)
            }
    }

}
