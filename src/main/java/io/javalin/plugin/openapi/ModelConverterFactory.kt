package io.javalin.plugin.openapi

import io.swagger.v3.core.converter.ModelConverter

/**
 * Creates a model converter, which converts a class to a open api schema.
 */
@FunctionalInterface
interface ModelConverterFactory {
    fun create(): ModelConverter
}
