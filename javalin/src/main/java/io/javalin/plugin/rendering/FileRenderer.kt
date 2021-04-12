/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.plugin.rendering

import io.javalin.http.Context

/**
 * Interface for creating renderers to be used with [Context.render].
 */
fun interface FileRenderer {
    @Throws(Exception::class)
    fun render(filePath: String, model: Map<String, Any?>, context: Context): String
}
