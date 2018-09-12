/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.core.util.SwaggerRenderer

fun main(args: Array<String>) {
    val app = Javalin.create().apply {
        enableWebJars()
        get("/my-docs", SwaggerRenderer("exampleApiSpec.yaml"))
    }.start(0)
}
