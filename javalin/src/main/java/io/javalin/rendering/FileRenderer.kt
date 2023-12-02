/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.rendering

import io.javalin.hook.Hook
import io.javalin.http.Context

/** Interface for creating renderers to be used with [Context.render].  */
fun interface FileRenderer {
    companion object {
        @JvmField val UseFileRenderer = Hook<FileRenderer>("javalin-file-renderer")
    }

    /** Renders the given file  */
    fun render(filePath: String, model: Map<String, Any?>, context: Context): String
}

class NotImplementedRenderer : FileRenderer {
    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        throw UnsupportedOperationException("No FileRenderer configured. You can configure one in config.fileRenderer(...)")
    }
}
