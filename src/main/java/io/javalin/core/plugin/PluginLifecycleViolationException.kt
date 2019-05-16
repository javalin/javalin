package io.javalin.core.plugin

abstract class PluginLifecycleViolationException(
        open val pluginClass: Class<out Plugin>,
        override val message: String
) : RuntimeException("Error in ${pluginClass.canonicalName}: $message")

data class PluginInitLifecycleViolationException(override val pluginClass: Class<out Plugin>) : PluginLifecycleViolationException(
        pluginClass,
        "It is not allowed to register handlers during the 'Init' lifecycle. Please use the 'Apply' lifecycle instead."
)
