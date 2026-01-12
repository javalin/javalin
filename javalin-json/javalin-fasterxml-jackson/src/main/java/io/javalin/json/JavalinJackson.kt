/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.util.javalinLazy
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * JsonMapper implementation using Jackson 2.x (com.fasterxml.jackson).
 * 
 * This mapper automatically registers optional Jackson modules if they are on the classpath:
 * - jackson-module-kotlin
 * - jackson-datatype-jsr310
 * - jackson-datatype-eclipse-collections
 */
class JavalinJackson(
    objectMapper: ObjectMapper? = null,
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private val pipedStreamExecutor: PipedStreamExecutor by javalinLazy { PipedStreamExecutor(useVirtualThreads) }

    private val _mapper: ObjectMapper = objectMapper ?: defaultMapper()

    val mapper: ObjectMapper get() = _mapper

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj // the default mapper treats strings as if they are already JSON
        else -> mapper.writeValueAsString(obj) // convert object to JSON
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
        else -> pipedStreamExecutor.getInputStream { pipedOutputStream ->
            mapper.factory.createGenerator(pipedOutputStream).writeObject(obj)
        }
    }

    override fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream) {
        mapper.writer().writeValuesAsArray(outputStream).use { sequenceWriter ->
            stream.forEach { sequenceWriter.write(it) }
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        mapper.readValue(json, mapper.typeFactory.constructType(targetType))

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        mapper.readValue(json, mapper.typeFactory.constructType(targetType))

    /** Update the current mapper and return self for easy chaining */
    fun updateMapper(updateFunction: Consumer<ObjectMapper>): JavalinJackson {
        updateFunction.accept(this.mapper)
        return this
    }

    companion object {
        private val OPTIONAL_MODULES = listOf(
            "com.fasterxml.jackson.module.kotlin.KotlinModule",
            "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
            "com.fasterxml.jackson.datatype.eclipsecollections.EclipseCollectionsModule",
            "org.ktorm.jackson.KtormModule"
        )

        @JvmStatic
        fun defaultMapper(): ObjectMapper = ObjectMapper().apply {
            OPTIONAL_MODULES.forEach { registerOptionalModule(it) }
        }

        private fun ObjectMapper.registerOptionalModule(className: String) {
            try {
                val moduleClass = Class.forName(className)
                this.registerModule(moduleClass.getConstructor().newInstance() as Module)
            } catch (e: ClassNotFoundException) {
                // Module not on classpath, skip
            }
        }
    }
}

