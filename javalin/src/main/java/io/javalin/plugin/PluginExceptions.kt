package io.javalin.plugin

abstract class PluginException(pluginClass: Class<out Plugin<*>>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginAlreadyRegisteredException(val plugin: Plugin<*>) :
    PluginException(plugin::class.java, "${plugin.name()} is already registered")

data class PluginKeyAlreadyRegisteredException(val plugin: Plugin<*>) :
    PluginException(plugin::class.java, "${plugin.name()} is already registered with the same key")

class PluginNotRegisteredException : IllegalStateException("Requested plugin was not registered at startup")
