/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.json.JavalinGson

fun main() {
    Javalin.create { it.jsonMapper(JavalinGson()) }
        .get("/") { it.json(listOf("a", "b", "c")) }
        .start(7070)
}
