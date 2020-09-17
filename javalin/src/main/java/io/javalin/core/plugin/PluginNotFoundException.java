package io.javalin.core.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PluginNotFoundException extends RuntimeException {

    private final Class<? extends Plugin> pluginClass;

    public PluginNotFoundException(@NotNull Class<? extends Plugin> pluginClass) {
        super("The plugin " + pluginClass.getCanonicalName() + " was not found");
        this.pluginClass = pluginClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginNotFoundException that = (PluginNotFoundException) o;
        return pluginClass.equals(that.pluginClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginClass);
    }
}
