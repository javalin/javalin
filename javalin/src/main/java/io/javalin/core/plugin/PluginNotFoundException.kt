package io.javalin.core.plugin

data class PluginNotFoundException(val pluginClass: Class<out Plugin>)
    : RuntimeException("The plugin ${pluginClass.canonicalName} was not found")
