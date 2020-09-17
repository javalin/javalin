package io.javalin.core.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class PluginLifecycleViolationException extends RuntimeException {

    private final Class<? extends Plugin> pluginClass;

    public PluginLifecycleViolationException(@NotNull Class<? extends Plugin> pluginClass, String message) {
        super("Error in " + pluginClass.getCanonicalName() + " " + message);
        this.pluginClass = pluginClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginLifecycleViolationException)) return false;
        PluginLifecycleViolationException that = (PluginLifecycleViolationException) o;
        return pluginClass.equals(that.pluginClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginClass);
    }
}

