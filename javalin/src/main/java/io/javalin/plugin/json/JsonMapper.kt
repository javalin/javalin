/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.json

import io.javalin.Javalin
import io.javalin.http.Context

interface JsonMapper {
    fun <T> fromJson(json: String, targetClass: Class<T>): T
    fun toJson(obj: Any): String
}

fun Javalin.jsonMapper() = this.attribute<JsonMapper>("json-mapper")
fun Context.jsonMapper(): JsonMapper = this.appAttribute("json-mapper")
