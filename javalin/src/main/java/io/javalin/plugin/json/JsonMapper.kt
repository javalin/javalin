/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.json

import io.javalin.Javalin
import io.javalin.http.Context

const val JSON_MAPPER_KEY = "global-json-mapper"
fun Javalin.jsonMapper(): JsonMapper = this.attribute(JSON_MAPPER_KEY)
fun Context.jsonMapper(): JsonMapper = this.appAttribute(JSON_MAPPER_KEY)
