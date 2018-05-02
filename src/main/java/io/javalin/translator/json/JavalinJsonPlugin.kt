/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import io.javalin.core.util.Util

object JavalinJsonPlugin {

    @JvmStatic
    var mapper: Any? = null

    @JvmStatic
    var jsonToObjectMapper = object : JsonToObjectMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T {
            Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
            JavalinJacksonPlugin.objectMapper = JavalinJacksonPlugin.objectMapper ?: JavalinJacksonPlugin.createObjectMapper()
            return JavalinJacksonPlugin.objectMapper!!.readValue(json, targetClass)
        }
    }

    @JvmStatic
    var objectToJsonMapper = object : ObjectToJsonMapper {
        override fun map(obj: Any): String {
            Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
            JavalinJacksonPlugin.objectMapper = JavalinJacksonPlugin.objectMapper ?: JavalinJacksonPlugin.createObjectMapper()
            return JavalinJacksonPlugin.objectMapper!!.writeValueAsString(obj)
        }
    }

}

@FunctionalInterface
interface JsonToObjectMapper {
    fun <T> map(json: String, targetClass: Class<T>): T
}

@FunctionalInterface
interface ObjectToJsonMapper {
    fun map(obj: Any): String
}
