package io.javalin.core.plugin;

import io.javalin.Javalin;
import org.jetbrains.annotations.NotNull;

/** Extend [Plugin] with a new lifecycle */
public interface PluginLifecycleInit {
    /**
     * Initialize properties and event listener.
     * This will be called before any handler is registered.
     * It is not allowed to register handler during this lifecycle.
     */
    void init(@NotNull Javalin app);
}
