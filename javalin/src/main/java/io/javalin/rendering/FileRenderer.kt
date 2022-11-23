/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.rendering

import io.javalin.http.Context

/**
 * Interface for creating renderers to be used with [Context.render].
 */
fun interface FileRenderer {
    fun render(filePath: String, model: Map<String, Any?>, context: Context): String
}

const val FILE_RENDERER_KEY = "javalin-file-renderer"
fun Context.fileRenderer(): FileRenderer = this.appAttribute(FILE_RENDERER_KEY)
