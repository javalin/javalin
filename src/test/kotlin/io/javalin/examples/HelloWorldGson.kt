/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import com.google.gson.GsonBuilder
import io.javalin.Javalin
import io.javalin.json.JavalinJsonPlugin
import io.javalin.json.JsonToObjectMapper
import io.javalin.json.ObjectToJsonMapper
import java.util.*

fun main(args: Array<String>) {

    val gson = GsonBuilder().create()

    JavalinJsonPlugin.jsonToObjectMapper = object : JsonToObjectMapper {
        override fun <T> map(json: String, targetClass: Class<T>): T {
            return gson.fromJson(json, targetClass)
        }
    }

    JavalinJsonPlugin.objectToJsonMapper = object : ObjectToJsonMapper {
        override fun map(obj: Any): String {
            return gson.toJson(obj)
        }
    }

    val app = Javalin.create().port(7070).start()
    app.get("/") { ctx -> ctx.json(Arrays.asList("a", "b", "c")) }

}
