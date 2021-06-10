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

object JavalinJackson {

    private var objectMapper: ObjectMapper? = null
    private val defaultObjectMapper: ObjectMapper by lazy { defaultObjectMapper() }

    @JvmStatic
    fun configure(staticObjectMapper: ObjectMapper) {
        objectMapper = staticObjectMapper
    }

    @JvmStatic
    fun getObjectMapper(): ObjectMapper {
        return (objectMapper ?: defaultObjectMapper)
    }

    @JvmStatic
    fun defaultObjectMapper(): ObjectMapper = try {
        val className = OptionalDependency.JACKSON_KT.testClass
        ObjectMapper().registerModule(Class.forName(className).getConstructor().newInstance() as Module)
    } catch (e: ClassNotFoundException) {
        ObjectMapper()
    }

    val defaultToMapper = object: ToJsonMapper {
        override fun map(obj: Any): String = when (obj) {
            is String -> obj // the default mapper treats strings as if they are already JSON
            else -> toJson(obj) // convert object to JSON
        }
    }

    fun toJson(value: Any): String {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        return (objectMapper ?: defaultObjectMapper).writeValueAsString(value)
    }

    val defaultFromMapper = object : FromJsonMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T = fromJson(json, targetClass)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        if (Util.isKotlinClass(clazz)) {
            Util.ensureDependencyPresent(OptionalDependency.JACKSON_KT)
        }
        return (objectMapper ?: defaultObjectMapper).readValue(json, clazz)
    }

}
