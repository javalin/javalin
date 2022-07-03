/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.core.util.CoreDependency
import io.javalin.core.util.DependencyUtil
import io.javalin.core.util.Util
import java.io.InputStream

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

    override fun <T : Any> fromJsonString(json: String, targetClass: Class<T>): T {
        ensureDependenciesPresent(targetClass)
        return objectMapper!!.readValue(json, targetClass)
    }

    override fun <T : Any> fromJsonStream(json: InputStream, targetClass: Class<T>): T {
        ensureDependenciesPresent(targetClass)
        return objectMapper!!.readValue(json, targetClass)
    }

    private fun ensureDependenciesPresent(targetClass: Class<*>? = null) {
        DependencyUtil.ensurePresence(CoreDependency.JACKSON)
        if (targetClass != null && Util.isKotlinClass(targetClass)) {
            DependencyUtil.ensurePresence(CoreDependency.JACKSON_KT)
        }
        objectMapper = objectMapper ?: defaultMapper()
    }

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
