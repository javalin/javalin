/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.json.JsonMapper
import java.lang.reflect.Type

fun main() {
    val rawJsonMapper: JsonMapper = object : JsonMapper {
        // serialize obj your favourite api
        override fun toJsonString(obj: Any, type: Type): String = "{ \"" + type.typeName + "\": \"" + obj + "\" }"

        // deserialize json your favourite api
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> fromJsonString(json: String, targetType: Type): T = when (targetType) {
            String::class.java -> json as T
            else -> throw UnsupportedOperationException("RawJsonMapper can deserialize only strings")
        }
    }

    Javalin.create {
        it.jsonMapper(rawJsonMapper)
        it.routes.get("/") { it.json(listOf("a", "b", "c")) }
    }.start(7070)
}
