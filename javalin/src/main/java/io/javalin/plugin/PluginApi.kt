package io.javalin.plugin

import io.javalin.config.JavalinConfig
import java.util.function.Consumer

abstract class JavalinPlugin<CFG> @JvmOverloads constructor(
    /** DSL component **/
    dsl: PluginFactory<out JavalinPlugin<CFG>, CFG>,
    /** Configuration state that will be passed to user config consumer **/
    defaultConfiguration: CFG,
    /** User configuration consumer **/
    userConfig: Consumer<CFG> = Consumer {},
    /** Plugin name, by default it is the simple name of the plugin class **/
    private val name: String? = null,
    /** Plugin priority, by default it is [PluginPriority.NORMAL] **/
    private val priority: PluginPriority = PluginPriority.NORMAL,
    /** If the plugin can be registered multiple times, by default it is false **/
    private val repeatable: Boolean = false
) {

    /** Plugin configuration **/
    protected val pluginConfig = defaultConfiguration.also { userConfig.accept(it) }

    /**
     * Initialize properties and access configuration before any handler is registered.
     */
    open fun onInitialize(config: JavalinConfig) {}

    /**
     * Called when the plugin is applied to the Javalin instance.
     */
    open fun onStart(config: JavalinConfig) {}

    /**
     * Checks if plugin can be registered multiple times.
     */
    fun repeatable(): Boolean = repeatable

    /**
     * The priority of the plugin that determines when it should be started.
     */
    fun priority(): PluginPriority = priority

    /**
     * The name of this plugin.
     */
    fun name(): String = name ?: this::class.java.simpleName

}

fun interface PluginFactory<PLUGIN : JavalinPlugin<CFG>, CFG> {

    /**
     * Create a new instance of the plugin with the given configuration.
     */
    fun create(config: Consumer<CFG>): PLUGIN

}

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
