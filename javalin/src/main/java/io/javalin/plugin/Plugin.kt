package io.javalin.plugin

import io.javalin.Javalin
import io.javalin.config.JavalinConfig

enum class PluginPriority {
    /**
     * Plugins with priority EARLY will be initialized before other type of plugins.
     * These plugins should be focused on meta configuration and not on registering handlers.
     */
    EARLY,
    /**
     * Plugins with priority NORMAL will be initialized after EARLY plugins and before LATE plugins.
     * This is a good default priority for most plugins.
     */
    NORMAL,
    /**
     * Plugins with priority LATE will be initialized after other type of plugins.
     * These plugins should be focused on meta analysis of the overall setup.
     */
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
