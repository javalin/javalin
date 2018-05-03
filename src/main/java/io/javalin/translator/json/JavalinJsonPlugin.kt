/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.json

import io.javalin.core.util.Util

@FunctionalInterface
interface JsonToObjectMapper {
    fun <T> map(json: String, targetClass: Class<T>): T
}

@FunctionalInterface
interface ObjectToJsonMapper {
    fun map(obj: Any): String
}

object JavalinJsonPlugin {

    @JvmStatic
    var jsonToObjectMapper = object : JsonToObjectMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T { // this awkward implementation is for backwards compatibility
            Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
            JavalinJacksonPlugin.objectMapper = JavalinJacksonPlugin.objectMapper ?: JavalinJacksonPlugin.createObjectMapper()
            return JavalinJacksonPlugin.objectMapper!!.readValue(json, targetClass)
        }
    }

    @JvmStatic
    var objectToJsonMapper = object : ObjectToJsonMapper {
        override fun map(obj: Any): String { // this awkward implementation is for backwards compatibility
            Util.ensureDependencyPresent("Jackson", "com.fasterxml.jackson.databind.ObjectMapper", "com.fasterxml.jackson.core/jackson-databind")
            JavalinJacksonPlugin.objectMapper = JavalinJacksonPlugin.objectMapper ?: JavalinJacksonPlugin.createObjectMapper()
            return JavalinJacksonPlugin.objectMapper!!.writeValueAsString(obj)
        }
    }

}
