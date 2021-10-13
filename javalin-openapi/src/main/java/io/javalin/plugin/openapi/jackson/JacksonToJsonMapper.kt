package io.javalin.plugin.openapi.jackson

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.javalin.plugin.openapi.utils.LazyDefaultValue
import io.swagger.v3.core.jackson.mixin.MediaTypeMixin
import io.swagger.v3.core.jackson.mixin.SchemaMixin
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema

/**
 * Default jackson mapper for creating the object api schema json.
 * This enables some of the options that are required to work with the UIs.
 */
class JacksonToJsonMapper(
        objectMapper: ObjectMapper? = null
) : ToJsonMapper {
    val objectMapper: ObjectMapper by LazyDefaultValue {
        objectMapper ?: defaultObjectMapper
    }

    companion object {
        val defaultObjectMapper: ObjectMapper by LazyDefaultValue {
            Json.mapper().registerModule(kotlinModule())
                    .addMixIn(Schema::class.java, SchemaMixin::class.java)
                    .addMixIn(MediaType::class.java, MediaTypeMixin::class.java)
        }
    }


    override fun map(obj: Any): String = objectMapper.writeValueAsString(obj)
}
