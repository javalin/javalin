/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 * Test-only copy of JavalinJackson for core module tests.
 * The real implementation is in javalin-fasterxml-jackson module.
 */

package io.javalin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.stream.Stream

class JavalinJackson(
    objectMapper: ObjectMapper? = null,
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private val executorService by javalinLazy { ConcurrencyUtil.executorService("JavalinJsonThreadPool", useVirtualThreads) }

    private val _mapper: ObjectMapper = objectMapper ?: defaultMapper()

    val mapper: ObjectMapper get() = _mapper

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj
        else -> mapper.writeValueAsString(obj)
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream()
        else -> getInputStream { pipedOutputStream ->
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

    fun updateMapper(updateFunction: Consumer<ObjectMapper>): JavalinJackson {
        updateFunction.accept(this.mapper)
        return this
    }

    private fun getInputStream(userCallback: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = object : PipedInputStream(pipedOutputStream) {
            var exception: Exception? = null
            override fun close() = exception?.let { throw it } ?: super.close()
        }
        executorService.execute {
            try {
                userCallback(pipedOutputStream)
            } catch (userException: Exception) {
                pipedInputStream.exception = userException
            } finally {
                pipedOutputStream.close()
            }
        }
        return pipedInputStream
    }

    companion object {
        private val OPTIONAL_MODULES = listOf(
            "com.fasterxml.jackson.module.kotlin.KotlinModule",
            "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule",
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

