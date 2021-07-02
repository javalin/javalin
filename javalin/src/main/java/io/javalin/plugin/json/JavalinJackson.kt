/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import java.io.ByteArrayOutputStream
import java.io.InputStream

val defaultMapper by lazy {
    try {
        val className = OptionalDependency.JACKSON_KT.testClass
        ObjectMapper().registerModule(Class.forName(className).getConstructor().newInstance() as Module)
    } catch (e: ClassNotFoundException) {
        ObjectMapper()
    }
}

class JavalinJackson(val objectMapper: ObjectMapper = defaultMapper) : JsonMapper {

    override fun toJson(obj: Any): String {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        return when (obj) {
            is String -> obj // the default mapper treats strings as if they are already JSON
            else -> objectMapper.writeValueAsString(obj) // convert object to JSON
        }
    }

    override fun toJsonStream(obj: Any): InputStream {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        val outputStream = ByteArrayOutputStream().apply {
            objectMapper.factory.createGenerator(this).writeObject(obj)
        }
        return PipedUtil.convertInputStreamToOutputStream(outputStream)
    }

    override fun <T> fromJson(json: String, targetClass: Class<T>): T {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        if (Util.isKotlinClass(targetClass)) {
            Util.ensureDependencyPresent(OptionalDependency.JACKSON_KT)
        }
        return objectMapper.readValue(json, targetClass)
    }

}
