/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper

object JavalinJacksonPlugin {

    private var objectMapper: ObjectMapper? = null

    private fun createObjectMapper(): ObjectMapper = try {
        val className = "com.fasterxml.jackson.module.kotlin.KotlinModule";
        ObjectMapper().registerModule(Class.forName(className)
                .getConstructor()
                .newInstance() as Module)
    } catch (e: ClassNotFoundException) {
        ObjectMapper()
    }

    @JvmStatic
    fun configure(staticObjectMapper: ObjectMapper) {
        objectMapper = staticObjectMapper
    }

    fun toJson(`object`: Any): String {
        objectMapper = objectMapper ?: createObjectMapper()
        return objectMapper!!.writeValueAsString(`object`)
    }

    fun <T> toObject(json: String, clazz: Class<T>): T {
        objectMapper = objectMapper ?: createObjectMapper()
        return objectMapper!!.readValue(json, clazz)
    }

}
