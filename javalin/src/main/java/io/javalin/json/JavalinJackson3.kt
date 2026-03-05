/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.javalinLazy
import tools.jackson.core.StreamWriteFeature
import tools.jackson.databind.JacksonModule
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.stream.Stream
import tools.jackson.databind.json.JsonMapper as Jackson3Mapper

class JavalinJackson3(
    private var jsonMapper: Jackson3Mapper? = null,
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private var mapperInstance: Jackson3Mapper? = null

    private val pipedStreamExecutor: PipedStreamExecutor by javalinLazy { PipedStreamExecutor(useVirtualThreads) }

    private val mapperDelegate: Lazy<Any> = javalinLazy {
        if (!Util.classExists(CoreDependency.JACKSON3.testClass)) {
            val message =
                """|It looks like you don't have Jackson 3 dependency on classpath.
                   |The easiest way to fix this is to simply add the '${CoreDependency.JACKSON3.artifactId}' dependency:
                   |
                   |${DependencyUtil.mavenAndGradleSnippets(CoreDependency.JACKSON3)}
                   |
                   |If you're using Kotlin, you will need to add '${CoreDependency.JACKSON3_KT.artifactId}'.
                   |
                   |To use a different JSON mapper, visit https://javalin.io/documentation#configuring-the-json-mapper""".trimMargin()
            JavalinLogger.warn(DependencyUtil.wrapInSeparators(message))
            throw InternalServerErrorResponse(message)
        }
        (jsonMapper ?: defaultMapper()) as Any
    }

    val mapper: Jackson3Mapper
        get() = mapperInstance ?: mapperDelegate.value as Jackson3Mapper

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj // the default mapper treats strings as if they are already JSON
        else -> mapper.writeValueAsString(obj) // convert object to JSON
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
        else -> pipedStreamExecutor.getInputStream { pipedOutputStream ->
            mapper.writeValue(pipedOutputStream, obj)
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
    fun updateMapper(updateFunction: Consumer<Jackson3Mapper.Builder>): JavalinJackson3 {
        val jsonMapperBuilder = this.mapper.rebuild()
        updateFunction.accept(jsonMapperBuilder)
        mapperInstance = jsonMapperBuilder.build()
        return this
    }


    companion object {
        @JvmStatic
        fun defaultMapper(): Jackson3Mapper = Jackson3Mapper.builder()
            .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
            .registerOptionalModule(CoreDependency.JACKSON3_KT.testClass)
            .registerOptionalModule(CoreDependency.JACKSON3_ECLIPSE_COLLECTIONS.testClass)
            .build()
    }
}

private fun Jackson3Mapper.Builder.registerOptionalModule(classString: String): Jackson3Mapper.Builder {
    if (Util.classExists(classString)) {
        this.addModule(Class.forName(classString).getConstructor().newInstance() as JacksonModule)
    }
    return this
}
