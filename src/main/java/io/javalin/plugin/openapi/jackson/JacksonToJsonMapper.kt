package io.javalin.plugin.openapi.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.javalin.plugin.json.ToJsonMapper
import io.swagger.v3.oas.models.security.SecurityScheme

/**
 * Default jackson mapper for creating the object api schema json.
 * This enables some of the options that are required to work with the uis.
 */
object JacksonToJsonMapper : ToJsonMapper {
    val objectMapper by lazy {
        jacksonObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(SimpleModule()
                        .addSerializer(SecurityScheme.Type::class.java, ToStringSerializer())
                        .addSerializer(SecurityScheme.In::class.java, ToStringSerializer()))
    }

    override fun map(obj: Any): String = objectMapper.writeValueAsString(obj)
}
