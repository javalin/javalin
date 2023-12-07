package io.javalin.plugin

import io.javalin.config.JavalinConfig
import io.javalin.util.parentClass
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

class PluginManager internal constructor(private val cfg: JavalinConfig) {

    private val plugins = mutableListOf<Plugin<*>>()
    private val initializedPlugins = mutableListOf<Plugin<*>>()
    private val enabledPlugins = mutableListOf<Plugin<*>>()
    private val contextPlugins = mutableMapOf<PluginKey<*>, ContextExtendingPlugin<*, *>>()
    private val contextPluginClasses = mutableMapOf<Class<*>, PluginKey<*>>()

    fun register(plugin: Plugin<*>) {
        if (!plugin.repeatable() && plugins.any { it.javaClass == plugin.javaClass }) {
            throw PluginAlreadyRegisteredException(plugin)
        }
        if(plugin is ContextExtendingPlugin<*, *>) {
            register(null, plugin)
        } else {
            plugins.add(plugin)
            initializePlugins()
        }
    }

    fun <T : ContextExtendingPlugin<*, *>> register(pluginKey: PluginKey<T>?, plugin: T) {
        var theKey = pluginKey
        // Ensure we map all superclasses that aren't ContextExtendingPlugin since we don't really know which one is "most correct"
        val theClasses: MutableList<Class<*>> = mutableListOf(plugin.javaClass)
        theClasses.addAll(plugin::class.supertypes.filter {  it.isSubtypeOf(typeOf<ContextExtendingPlugin<*,*>>()) && it.classifier != ContextExtendingPlugin::class }.map { it.jvmErasure.java })
        if(theClasses.any { contextPluginClasses.containsKey(it) }) {
            throw PluginKeyAlreadyRegisteredException(plugin)
        }
        if (theKey == null) {
            theKey = PluginKey()
            theClasses.forEach {
                contextPluginClasses[it] = theKey
            }
            // If using class keys make sure it wasn't registered without a class key
            if (plugins.any { it.javaClass == plugin.javaClass }) {
                throw PluginAlreadyRegisteredException(plugin)
            }
        } else if (contextPlugins.containsKey(theKey)) {
            throw PluginKeyAlreadyRegisteredException(plugin)
        }
        plugins.add(plugin)
        initializePlugins()
        contextPlugins[theKey] = plugin
    }

    private fun initializePlugins() {
        while (plugins.size != initializedPlugins.size) {
            val pluginsToInitialize = plugins
                .asSequence()
                .filter { it !in initializedPlugins }
                .sortedBy { it.priority() }

            for (plugin in pluginsToInitialize) {
                initializedPlugins.add(plugin)
                plugin.onInitialize(cfg)
            }
        }
    }

    fun startPlugins() {
        initializedPlugins
            .asSequence()
            .filter { it !in enabledPlugins }
            .sortedBy { it.priority() }
            .forEach {
                it.onStart(cfg)
                enabledPlugins.add(it)
            }
    }

    fun <T> fromKey(pluginKey: PluginKey<out ContextExtendingPlugin<*, T>>): ContextExtendingPlugin<*, T> {
        return (contextPlugins[pluginKey] ?: throw PluginNotRegisteredException()) as ContextExtendingPlugin<*, T>
    }

    fun <T> fromKey(clazz: Class<out ContextExtendingPlugin<*, T>>): ContextExtendingPlugin<*, T> {
        val pluginKey: PluginKey<ContextExtendingPlugin<*, T>> = (contextPluginClasses[clazz] ?: throw PluginNotRegisteredException()) as PluginKey<ContextExtendingPlugin<*, T>>
        return fromKey(pluginKey)
    }
}
