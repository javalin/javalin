package io.javalin.plugin

abstract class PluginException(pluginClass: Class<out Plugin>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginAlreadyRegisteredException(val pluginClass: Class<out Plugin>) :
    PluginException(pluginClass, "${pluginClass.canonicalName} is already registered")

data class PluginInitException(val pluginClass: Class<out Plugin>) :
    PluginException(pluginClass, "It is not allowed to register handlers during the 'Init' lifecycle. Please use the 'Apply' lifecycle instead.")
