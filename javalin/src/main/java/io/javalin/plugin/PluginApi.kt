package io.javalin.plugin

import io.javalin.config.JavalinConfig
import java.util.function.Consumer

enum class PluginPriority {
    /**
     * Plugins with priority EARLY will be started before other type of plugins.
     * These plugins should be focused on meta configuration and not on registering handlers.
     */
    EARLY,

    /**
     * Plugins with priority NORMAL will be started after EARLY plugins and before LATE plugins.
     * This is a good default priority for most plugins.
     */
    NORMAL,

    /**
     * Plugins with priority LATE will be started after other type of plugins.
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
    fun onStart(config: JavalinConfig)

    /**
     * Checks if plugin can be registered multiple times.
     */
    fun repeatable(): Boolean = false

    /**
     * The priority of the plugin that determines when it should be started.
     */
    fun priority(): PluginPriority = PluginPriority.NORMAL

    /**
     * The name of this plugin.
     */
    fun name(): String = this.javaClass.simpleName

}

abstract class Plugin<CONFIG>(userConfig: Consumer<CONFIG>? = null, defaultConfig: CONFIG) : JavalinPlugin {
    protected val pluginConfig = defaultConfig.also { userConfig?.accept(it) } // apply user config to default config if present
}

abstract class NoConfigPlugin : JavalinPlugin
