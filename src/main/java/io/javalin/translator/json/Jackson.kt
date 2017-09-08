/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import com.fasterxml.jackson.databind.ObjectMapper

object JavalinJacksonPlugin {

    private var objectMapper: ObjectMapper? = null

    @JvmStatic
    fun configure(staticObjectMapper: ObjectMapper) {
        objectMapper = staticObjectMapper
    }

    fun toJson(`object`: Any): String {
        objectMapper = objectMapper ?: ObjectMapper()
        return objectMapper!!.writeValueAsString(`object`)
    }

    fun <T> toObject(json: String, clazz: Class<T>): T {
        objectMapper = objectMapper ?: ObjectMapper()
        return objectMapper!!.readValue(json, clazz)
    }

}
