/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.HaltException
import org.slf4j.LoggerFactory

object Jackson {

    private val log = LoggerFactory.getLogger(Jackson::class.java)

    private var objectMapper: ObjectMapper? = null

    fun toJson(`object`: Any): String {
        if (objectMapper == null) {
            objectMapper = ObjectMapper()
        }
        try {
            return objectMapper!!.writeValueAsString(`object`)
        } catch (e: Exception) {
            val message = "Failed to write object as JSON"
            log.warn(message, e)
            throw HaltException(500, message)
        }

    }

    fun <T> toObject(json: String, clazz: Class<T>): T {
        if (objectMapper == null) {
            objectMapper = ObjectMapper()
        }
        try {
            return objectMapper!!.readValue(json, clazz)
        } catch (e: Exception) {
            val message = "Failed to convert JSON to " + clazz.name
            log.warn(message, e)
            throw HaltException(500, message)
        }

    }
}
