package io.javalin.core.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PluginAlreadyRegisteredException extends RuntimeException {

    private final Class<? extends Plugin> pluginClass;

    public PluginAlreadyRegisteredException(@NotNull Class<? extends Plugin> pluginClass) {
        super(pluginClass.getCanonicalName() +" is already registered");
        this.pluginClass = pluginClass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginAlreadyRegisteredException that = (PluginAlreadyRegisteredException) o;
        return pluginClass.equals(that.pluginClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pluginClass);
    }
}
