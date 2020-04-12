package io.javalin.plugin.openapi.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.plugin.openapi.ModelConverterFactory
import io.javalin.plugin.openapi.utils.LazyDefaultValue
import io.swagger.v3.core.converter.ModelConverter

/**
 * The default model converter, that uses jackson for the serialization.
 * This converter respects the jackson options and annotations.
 */
class JacksonModelConverterFactory(
        objectMapper: ObjectMapper? = null
) : ModelConverterFactory {
    val objectMapper: ObjectMapper by LazyDefaultValue {
        objectMapper ?: JacksonToJsonMapper.defaultObjectMapper
    }

    override fun create(): ModelConverter = JavalinModelResolver(objectMapper)
}
