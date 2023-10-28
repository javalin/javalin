package io.javalin.plugin

abstract class PluginException(pluginClass: Class<out Plugin<*>>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginAlreadyRegisteredException(val plugin: Plugin<*>) :
    PluginException(plugin::class.java, "${plugin.name()} is already registered")
