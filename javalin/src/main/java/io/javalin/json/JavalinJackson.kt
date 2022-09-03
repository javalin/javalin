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
import java.io.InputStream
import java.lang.reflect.Type

class JavalinJackson(private var objectMapper: ObjectMapper? = null) : JsonMapper {

    val mapper by lazy {
        if (!Util.classExists(CoreDependency.JACKSON.testClass)) {
            val message = DependencyUtil.missingDependencyMessage(CoreDependency.JACKSON)
            JavalinLogger.warn(message)
            message + "\nIf you're using Kotlin, you will also need to add '${CoreDependency.JACKSON_KT.artifactId}'"
            throw InternalServerErrorResponse(message)
        }
        objectMapper ?: defaultMapper()
    }

    override fun toJsonString(obj: Any, type: Type): String = when (obj) {
        is String -> obj // the default mapper treats strings as if they are already JSON
        else -> mapper.writeValueAsString(obj) // convert object to JSON
    }

    override fun toJsonStream(obj: Any, type: Type): InputStream = when (obj) {
        is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
        else -> PipedStreamUtil.getInputStream { pipedOutputStream ->
            mapper.factory.createGenerator(pipedOutputStream).writeObject(obj)
        }
    }

    override fun <T : Any> fromJsonString(json: String, targetType: Type): T =
        mapper.readValue(json, mapper.typeFactory.constructType(targetType))

    override fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T =
        mapper.readValue(json, mapper.typeFactory.constructType(targetType))

    companion object {
        fun defaultMapper(): ObjectMapper = ObjectMapper()
            .registerOptionalModule(CoreDependency.JACKSON_KT.testClass)
            .registerOptionalModule(CoreDependency.JACKSON_JSR_310.testClass)
            .registerOptionalModule(CoreDependency.JACKSON_KTORM.testClass) // very optional module for ktorm (a kotlin orm)
    }
}

private fun ObjectMapper.registerOptionalModule(classString: String): ObjectMapper {
    if (Util.classExists(classString)) {
        this.registerModule(Class.forName(classString).getConstructor().newInstance() as Module)
    }
    return this
}
