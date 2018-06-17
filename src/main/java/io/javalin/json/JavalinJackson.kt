/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util

object JavalinJackson {

    private var objectMapper: ObjectMapper? = null

    @JvmStatic
    fun configure(staticObjectMapper: ObjectMapper) {
        objectMapper = staticObjectMapper
    }

    fun toJson(`object`: Any): String {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        objectMapper = objectMapper ?: createObjectMapper()
        return objectMapper!!.writeValueAsString(`object`)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        Util.ensureDependencyPresent(OptionalDependency.JACKSON)
        objectMapper = objectMapper ?: createObjectMapper()
        return objectMapper!!.readValue(json, clazz)
    }

    private fun createObjectMapper(): ObjectMapper = try {
        val className = "com.fasterxml.jackson.module.kotlin.KotlinModule"
        ObjectMapper().registerModule(Class.forName(className).getConstructor().newInstance() as Module)
    } catch (e: ClassNotFoundException) {
        ObjectMapper()
    }

}
