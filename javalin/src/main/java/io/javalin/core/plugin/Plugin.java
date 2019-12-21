package io.javalin.core.plugin;

import io.javalin.Javalin;
import io.javalin.core.JavalinConfig;
import org.jetbrains.annotations.NotNull;

/**
 * A extension is a modular way of adding functionality to a Javalin instance.
 * Lifecycle interfaces can be used to listen to specific callbacks.
 * To apply a plugin use {@link JavalinConfig#registerPlugin(Plugin)}.
 */
@FunctionalInterface
public interface Plugin {
    /**
     * Configure the Javalin instance and register handler
     */
    void apply(@NotNull Javalin app);
}
