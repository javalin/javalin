package io.javalin.core.plugin

data class PluginAlreadyRegisteredException(val pluginClass: Class<out Plugin>) :
    RuntimeException("${pluginClass.canonicalName} is already registered")

data class PluginNotFoundException(val pluginClass: Class<out Plugin>) :
    RuntimeException("The plugin ${pluginClass.canonicalName} was not found")

abstract class PluginLifecycleViolationException(pluginClass: Class<out Plugin>, override val message: String) :
    RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginInitLifecycleViolationException(val pluginClass: Class<out Plugin>) :
    PluginLifecycleViolationException(pluginClass, "It is not allowed to register handlers during the 'Init' lifecycle. Please use the 'Apply' lifecycle instead.")
