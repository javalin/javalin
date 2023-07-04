package io.javalin.plugin

abstract class PluginException(pluginClass: Class<out JavalinPlugin>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginAlreadyRegisteredException(val plugin: JavalinPlugin) :
    PluginException(plugin::class.java, "${plugin.name()} is already registered")
