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
            plugins
                .filterNot { enabledPlugins.contains(it) }
                .filterNot { initializedPlugins.contains(it) }
                .forEach {
                    it.onInitialize(cfg)
                    initializedPlugins.add(it)
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
