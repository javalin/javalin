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
import java.io.InputStream

private fun defaultMapper(): ObjectMapper = try {
    val className = OptionalDependency.JACKSON_KT.testClass
    ObjectMapper().registerModule(Class.forName(className).getConstructor().newInstance() as Module)
} catch (e: ClassNotFoundException) {
    ObjectMapper()
}

class JavalinJackson(private var objectMapper: ObjectMapper? = null) : JsonMapper {

    override fun toJsonString(obj: Any): String {
        ensureDependenciesPresent()
        return when (obj) {
            is String -> obj // the default mapper treats strings as if they are already JSON
            else -> objectMapper!!.writeValueAsString(obj) // convert object to JSON
        }
    }

    override fun toJsonStream(obj: Any): InputStream {
        ensureDependenciesPresent()
        return when (obj) {
            is String -> obj.byteInputStream() // the default mapper treats strings as if they are already JSON
            else -> PipedStreamUtil.getInputStream { pipedOutputStream ->
                objectMapper!!.factory.createGenerator(pipedOutputStream).writeObject(obj)
            }
        }
    }

    override fun <T> fromJsonString(json: String, targetClass: Class<T>): T {
        ensureDependenciesPresent(targetClass)
        return objectMapper!!.readValue(json, targetClass)
    }

    override fun <T : Any?> fromJsonStream(json: InputStream, targetClass: Class<T>): T {
        ensureDependenciesPresent(targetClass)
        return objectMapper!!.readValue(json, targetClass)
    }

    private fun ensureDependenciesPresent(targetClass: Class<*>? = null) {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        if (targetClass != null && Util.isKotlinClass(targetClass)) {
            Util.ensureDependencyPresent(OptionalDependency.JACKSON_KT)
        }
        objectMapper = objectMapper ?: defaultMapper()
    }

}
