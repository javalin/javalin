package io.javalin.json

import io.javalin.util.JavalinLogger
import java.io.InputStream
import java.lang.reflect.Type

class UnsupportedJsonMapper : JsonMapper {

    private fun unsupportedException(): Nothing {
        val errorMessage = """
        |--------------------------------------------------------------------------------------
        |JsonMapper is currently disabled and you cannot serialize & deserialize Java objects.
        |To enable JsonMapper, define your mapper in Javalin configuration.
        |Available mappers:
        |cfg.jsonMapper(new DisabledJsonMapper()); // current
        |cfg.jsonMapper(new JacksonJsonMapper()); // requires Jackson dependency
        |cfg.jsonMapper(new GsonJsonMapper()); // requires Gson dependency
        |
        |You can also implement your own mapper. For more details visit:
        |* https://javalin.io/documentation#configuring-the-json-mapper
        |--------------------------------------------------------------------------------------
        """.trimMargin()

        JavalinLogger.error(errorMessage)
        throw UnsupportedOperationException(errorMessage)
    }

    override fun toJsonString(obj: Any, type: Type): String = unsupportedException()
    override fun toJsonStream(obj: Any, type: Type): InputStream = unsupportedException()
    override fun <T : Any> fromJsonString(json: String, targetType: Type): T = unsupportedException()
    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T = unsupportedException()

}
