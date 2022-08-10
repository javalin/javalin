package io.javalin.plugin

import io.javalin.Javalin
import java.util.function.Consumer

internal object PluginUtil {
    fun attachPlugins(app: Javalin, plugins: Collection<Plugin>) {
        var anyHandlerAdded = false
        app.events { event ->
            event.handlerAdded { anyHandlerAdded = true }
            event.wsHandlerAdded { anyHandlerAdded = true }
        }
        plugins.filterIsInstance<PluginLifecycleInit>().forEach(Consumer { initPlugin ->
            initPlugin.init(app)
            if (anyHandlerAdded) { // check if any "init" added a handler
                throw PluginInitException((initPlugin as Plugin).javaClass)
            }
        })
        plugins.forEach { plugin -> plugin.apply(app) }
    }
}
