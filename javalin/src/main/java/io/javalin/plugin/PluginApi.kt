package io.javalin.plugin

import io.javalin.config.JavalinState
import io.javalin.http.Context
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


/**
 * Extend this class to create a plugin with a config.
 * The config is created by combining a default config and a user config.
 * The combined config is available as [pluginConfig] to the extending class.
 */
abstract class Plugin<CONFIG>(userConfig: Consumer<CONFIG>? = null, defaultConfig: CONFIG? = null) {

    /** Initialize properties and access configuration before any handler is registered. */
    open fun onInitialize(config: JavalinState) {}

    /** Called when the plugin is applied to the Javalin instance. */
    open fun onStart(config: JavalinState) {}

    /**Checks if plugin can be registered multiple times. */
    open fun repeatable(): Boolean = false

    /** The priority of the plugin that determines when it should be started. */
    open fun priority(): PluginPriority = PluginPriority.NORMAL

    /** The name of this plugin. */
    open fun name(): String = this.javaClass.simpleName

    /** The combined config of the plugin. */
    @JvmField
    protected var pluginConfig: CONFIG = defaultConfig?.also { userConfig?.accept(it) } as CONFIG

}

abstract class ContextPlugin<CONFIG, EXTENSION>(
    userConfig: Consumer<CONFIG>? = null,
    defaultConfig: CONFIG? = null
) : Plugin<CONFIG>(userConfig, defaultConfig) {
    /** Context extending plugins cannot be repeatable, as they are keyed by class */
    final override fun repeatable(): Boolean = false
    abstract fun createExtension(context: Context): EXTENSION
}
