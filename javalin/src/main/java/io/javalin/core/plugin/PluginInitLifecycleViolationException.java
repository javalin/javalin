package io.javalin.core.plugin;

public class PluginInitLifecycleViolationException extends PluginLifecycleViolationException {

    public PluginInitLifecycleViolationException(Class<? extends Plugin> pluginClass) {
        super(pluginClass,
            "It is not allowed to register handlers during the 'Init' lifecycle. Please use the 'Apply' lifecycle instead.");
    }
}
