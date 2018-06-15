/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

@FunctionalInterface
interface JsonToObjectMapper {
    fun <T> map(json: String, targetClass: Class<T>): T
}

@FunctionalInterface
interface ObjectToJsonMapper {
    fun map(obj: Any): String
}

object JavalinJson {

    @JvmStatic
    var jsonToObjectMapper = object : JsonToObjectMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T = JavalinJackson.toObject(json, targetClass)
    }

    @JvmStatic
    var objectToJsonMapper = object : ObjectToJsonMapper {
        override fun map(obj: Any): String = JavalinJackson.toJson(obj)
    }

}
