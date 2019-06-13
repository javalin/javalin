package io.javalin.plugin.openapi.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.plugin.json.ToJsonMapper

/**
 * Default jackson mapper for creating the object api schema json.
 * This enables some of the options that are required to work with the uis.
 */
object JacksonToJsonMapper : ToJsonMapper {
    val objectMapper by lazy { jacksonObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL) }

    override fun map(obj: Any): String = objectMapper.writeValueAsString(obj)
}
