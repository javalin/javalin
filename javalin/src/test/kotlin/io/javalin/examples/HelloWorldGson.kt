/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.plugin.json.JsonMapper
import java.util.*

fun main() {

    val gson = GsonBuilder().create()

    val gsonMapper = object : JsonMapper {
        override fun <T> fromJsonString(json: String, targetClass: Class<T>): T = gson.fromJson(json, targetClass)
        override fun toJsonString(obj: Any) = gson.toJson(obj)
    }

    val app = Javalin.create { it.jsonMapper(gsonMapper) }.start(7070)
    app.get("/") { ctx -> ctx.json(Arrays.asList("a", "b", "c")) }

}
