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

    private var configuredOjectMapper: ObjectMapper? = null
    @JvmStatic
    val objectMapper : ObjectMapper by lazy {
        configuredOjectMapper ?: createObjectMapper()
    }

    @JvmStatic
    fun configure(staticObjectMapper: ObjectMapper) {
        configuredOjectMapper = staticObjectMapper
    }

    fun toJson(`object`: Any): String {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        return objectMapper.writeValueAsString(`object`)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        if (Util.isKotlinClass(clazz)) {
            Util.ensureDependencyPresent(OptionalDependency.JACKSON_KT)
        }
        return objectMapper.readValue(json, clazz)
    }

    private fun createObjectMapper(): ObjectMapper = try {
        val className = OptionalDependency.JACKSON_KT.testClass
        ObjectMapper().registerModule(Class.forName(className).getConstructor().newInstance() as Module)
    } catch (e: ClassNotFoundException) {
        ObjectMapper()
    }

}
