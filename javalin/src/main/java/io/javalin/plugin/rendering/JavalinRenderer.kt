/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering

import io.javalin.http.Context
import io.javalin.util.JavalinLogger

object JavalinRenderer {

    private val extensions = mutableMapOf<String, FileRenderer>()

    @JvmField
    var baseModelFunction: (Context) -> Map<String, Any?> = { mapOf<String, Any>() }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        val renderer = extensions[filePath.extension] ?: throw IllegalArgumentException("No Renderer registered for extension '${filePath.extension}'.")
        return renderer.render(filePath, baseModelFunction(ctx) + model, ctx) // overrides the base model
    }

    @JvmStatic
    fun register(fileRenderer: FileRenderer, vararg ext: String) = ext.forEach {
        if (extensions[it] != null) {
            JavalinLogger.info("'$it' is already registered to ${extensions[it]!!.javaClass}. Overriding.")
        }
        extensions[it] = fileRenderer
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
}
