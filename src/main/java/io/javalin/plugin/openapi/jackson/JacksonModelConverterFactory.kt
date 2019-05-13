package io.javalin.plugin.openapi.jackson

import io.javalin.plugin.openapi.ModelConverterFactory
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.core.jackson.ModelResolver

/**
 * The default model converter, that uses jackson for the serialization.
 * This converter respects the jackson options and annotations.
 */
object JacksonModelConverterFactory : ModelConverterFactory {
    override fun create(): ModelConverter = ModelResolver(JacksonToJsonMapper.objectMapper)
}
