/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.CoreDependency
import io.javalin.util.DependencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.javalinLazy
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.function.Consumer
import java.util.stream.Stream

class JavalinJackson(
    private var objectMapper: ObjectMapper? = null,
    private val useVirtualThreads: Boolean = false,
) : JsonMapper {

    private val pipedStreamExecutor: PipedStreamExecutor by javalinLazy { PipedStreamExecutor(useVirtualThreads) }

    private val mapperDelegate: Lazy<Any> = javalinLazy {
        if (!Util.classExists(CoreDependency.JACKSON.testClass)) {
            val message =
                """|It looks like you don't have an object mapper configured.
                   |The easiest way to fix this is to simply add the '${CoreDependency.JACKSON.artifactId}' dependency:
                   |
                   |${DependencyUtil.mavenAndGradleSnippets(CoreDependency.JACKSON)}
                   |
                   |If you're using Kotlin, you will need to add '${CoreDependency.JACKSON_KT.artifactId}'.
                   |
                   |To use a different object mapper, visit https://javalin.io/documentation#configuring-the-json-mapper""".trimMargin()
            JavalinLogger.warn(DependencyUtil.wrapInSeparators(message))
            throw InternalServerErrorResponse(message)
        }
        (objectMapper ?: defaultMapper()) as Any
    }

    val mapper: ObjectMapper get() = mapperDelegate.value as ObjectMapper

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
        @JvmStatic
        fun defaultMapper(): ObjectMapper = ObjectMapper()
            .registerOptionalModule(CoreDependency.JACKSON_KT.testClass)
            .registerOptionalModule(CoreDependency.JACKSON_JSR_310.testClass)
            .registerOptionalModule(CoreDependency.JACKSON_ECLIPSE_COLLECTIONS.testClass)
            .registerOptionalModule(CoreDependency.JACKSON_KTORM.testClass) // very optional module for ktorm (a kotlin orm)
    }
}

private fun ObjectMapper.registerOptionalModule(classString: String): ObjectMapper {
    if (Util.classExists(classString)) {
        this.registerModule(Class.forName(classString).getConstructor().newInstance() as Module)
    }
    return this
}
