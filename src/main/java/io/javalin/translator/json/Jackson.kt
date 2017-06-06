/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import com.fasterxml.jackson.databind.ObjectMapper

object Jackson {

    private var objectMapper: ObjectMapper? = null

    fun toJson(`object`: Any): String {
        objectMapper = objectMapper ?: ObjectMapper()
        try {
            return objectMapper!!.writeValueAsString(`object`)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun <T> toObject(json: String, clazz: Class<T>): T {
        objectMapper = objectMapper ?: ObjectMapper()
        try {
            return objectMapper!!.readValue(json, clazz)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

}
