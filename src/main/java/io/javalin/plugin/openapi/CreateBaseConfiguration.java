package io.javalin.plugin.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import org.jetbrains.annotations.NotNull;

/**
 * Create the base open api configuration.
 * This function will be called before the creation of every schema.
 */
@FunctionalInterface
public interface CreateBaseConfiguration {
    @NotNull OpenAPI create();
}
