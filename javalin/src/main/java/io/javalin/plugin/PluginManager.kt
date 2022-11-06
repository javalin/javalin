package io.javalin.plugin

import io.javalin.Javalin

class PluginManager {

    private val plugins: MutableMap<Class<out Plugin>, Plugin> = LinkedHashMap()
    private val initializedPlugins: MutableSet<Plugin> = mutableSetOf()

    fun register(plugin: Plugin) {
        if (plugins.containsKey(plugin.javaClass)) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        plugins[plugin.javaClass] = plugin
    }

    fun initializePlugins(app: Javalin) {
        var anyHandlerAdded = false

        app.events { event ->
            event.handlerAdded { anyHandlerAdded = true }
            event.wsHandlerAdded { anyHandlerAdded = true }
        }

        val pluginsToInitialize = plugins.values.filterNot { initializedPlugins.contains(it) }

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
