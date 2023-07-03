package io.javalin.plugin

abstract class PluginException(pluginClass: Class<out JavalinPlugin>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginAlreadyRegisteredException(val pluginClass: Class<out JavalinPlugin>) :
    PluginException(pluginClass, "${pluginClass.canonicalName} is already registered")
