package io.javalin.core.plugin

import java.lang.RuntimeException

data class PluginAlreadyRegisteredException(val pluginClass: Class<out Plugin>)
    : RuntimeException("${pluginClass.canonicalName} is already registered")
