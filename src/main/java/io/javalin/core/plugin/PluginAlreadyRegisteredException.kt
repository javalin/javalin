package io.javalin.core.plugin

data class PluginAlreadyRegisteredException(val pluginClass: Class<out Plugin>)
    : RuntimeException("${pluginClass.canonicalName} is already registered")
