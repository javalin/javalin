package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.config.JavalinConfig

enum class PluginPriority {
    EARLY,
    NORMAL,
    LATE
}

fun interface JavalinPlugin {

    /**
     * Initialize properties and access configuration before any handler is registered.
     */
    fun onInitialize(config: JavalinConfig) {}

    /**
     * Called when the plugin is applied to the Javalin instance.
     */
    fun onStart(app: Javalin)

    /**
     * Checks if plugin can be registered multiple times.
     */
    fun repeatable(): Boolean = false

    /**
     * The priority of this plugin.
     */
    fun priority(): PluginPriority = PluginPriority.NORMAL

    /**
     * The name of this plugin.
     */
    fun name(): String = this.javaClass.simpleName

}
