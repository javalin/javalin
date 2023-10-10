package io.javalin.plugin

import io.javalin.config.JavalinConfig
import java.util.function.Consumer

abstract class JavalinPlugin<CFG> @JvmOverloads constructor(
    dsl: PluginFactory<out JavalinPlugin<CFG>, CFG>,
    defaultConfiguration: CFG,
    userConfig: Consumer<CFG> = Consumer {},
    private val name: String? = null,
    private val priority: PluginPriority = PluginPriority.NORMAL,
    private val repeatable: Boolean = false
) {

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
