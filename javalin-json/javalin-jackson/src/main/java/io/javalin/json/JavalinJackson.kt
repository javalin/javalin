/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import io.javalin.util.javalinLazy
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.json.JsonMapper
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * JsonMapper implementation using Jackson 3.x (tools.jackson).
 *
 * This mapper automatically registers optional Jackson modules if they are on the classpath:
 * - jackson-module-kotlin
 * - jackson-datatype-jsr310
 * - jackson-datatype-eclipse-collections
 *
 * Jackson 3.x uses an immutable builder pattern for configuration.
 */
class JavalinJackson(
    jsonMapper: JsonMapper? = null,
    private val useVirtualThreads: Boolean = false,
) : io.javalin.json.JsonMapper {

    private val pipedStreamExecutor: PipedStreamExecutor by javalinLazy { PipedStreamExecutor(useVirtualThreads) }

    private val _mapper: JsonMapper = jsonMapper ?: defaultMapper()

    val mapper: JsonMapper get() = _mapper

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj // the default mapper treats strings as if they are already JSON
        else -> mapper.writeValueAsString(obj) // convert object to JSON
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
        else -> pipedStreamExecutor.getInputStream { pipedOutputStream ->
            mapper.createGenerator(pipedOutputStream).use { generator ->
                generator.writePOJO(obj)
            }
        }
    }

    override fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream) {
        mapper.writer().writeValuesAsArray(outputStream).use { sequenceWriter ->
            stream.forEach { sequenceWriter.write(it) }
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        mapper.readValue(json, mapper.constructType(targetType))

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        mapper.readValue(json, mapper.constructType(targetType))

    /**
     * Create a new JavalinJackson with an updated mapper configuration.
     * Since Jackson 3.x uses immutable mappers, this returns a new instance.
     */
    fun withMapper(updateFunction: Consumer<JsonMapper.Builder>): JavalinJackson {
        val builder = mapper.rebuild()
        updateFunction.accept(builder)
        return JavalinJackson(builder.build(), useVirtualThreads)
    }

    companion object {
        private val OPTIONAL_MODULES = listOf(
            "tools.jackson.module.kotlin.KotlinModule",
            "tools.jackson.datatype.jsr310.JavaTimeModule",
            "tools.jackson.datatype.eclipsecollections.EclipseCollectionsModule"
        )

        @JvmStatic
        fun defaultMapper(): JsonMapper {
            val builder = JsonMapper.builder()
            OPTIONAL_MODULES.forEach { className ->
                try {
                    val moduleClass = Class.forName(className)
                    val module = moduleClass.getConstructor().newInstance() as JacksonModule
                    builder.addModule(module)
                } catch (e: ClassNotFoundException) {
                    // Module not on classpath, skip
                }
            }
            return builder.build()
        }
    }
}

