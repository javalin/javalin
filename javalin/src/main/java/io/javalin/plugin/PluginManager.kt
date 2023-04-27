package io.javalin.plugin

import io.javalin.Javalin

class PluginManager {

    private val plugins: MutableList<Plugin> = mutableListOf()
    private val initializedPlugins: MutableSet<Plugin> = mutableSetOf()

    fun register(plugin: Plugin) {
        if (plugin !is RepeatablePlugin && plugins.any { it.javaClass == plugin.javaClass }) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        plugins.add(plugin)
    }

    fun initializePlugins(app: Javalin) {
        var anyHandlerAdded = false

        app.events { event ->
            event.handlerAdded { anyHandlerAdded = true }
            event.wsHandlerAdded { anyHandlerAdded = true }
        }

        val pluginsToInitialize = plugins.filterNot { initializedPlugins.contains(it) }

        pluginsToInitialize.forEach {
            if (it is PluginLifecycleInit) {
                it.init(app)

                if (anyHandlerAdded) { // check if any "init" added a handler
                    throw PluginInitException(it.javaClass)
                }
            }
        }

        pluginsToInitialize.forEach {
            it.apply(app)
            initializedPlugins.add(it)
        }
    }

}
