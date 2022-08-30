/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.json.JsonMapper
import java.lang.reflect.Type
import java.util.*

fun main() {
    val gson = GsonBuilder().create()

    val gsonMapper = object : JsonMapper {
        override fun <T : Any> fromJsonString(json: String, targetType: Type): T = gson.fromJson(json, targetType)
        override fun toJsonString(obj: Any, type: Type) = gson.toJson(obj)
    }

    val app = Javalin.create { it.jsonMapper(gsonMapper) }.start(7070)
    app.get("/") { it.json(Arrays.asList("a", "b", "c")) }
}
