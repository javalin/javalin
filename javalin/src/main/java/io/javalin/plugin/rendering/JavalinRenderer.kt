/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.plugin.rendering.markdown.JavalinCommonmark
import io.javalin.plugin.rendering.template.*

object JavalinRenderer {

    private val extensions = mutableMapOf<String, FileRenderer>()

    init {
        register(JavalinVelocity, ".vm", ".vtl")
        register(JavalinFreemarker, ".ftl")
        register(JavalinMustache, ".mustache")
        register(JavalinJtwig, ".jtwig", ".twig", ".html.twig")
        register(JavalinPebble, ".peb", ".pebble")
        register(JavalinThymeleaf, ".html", ".tl", ".thyme", ".thymeleaf")
        register(JavalinCommonmark, ".md", ".markdown")
        register(JavalinJte, ".jte")
    }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        val extension = if (filePath.hasTwoDots) filePath.doubleExtension else filePath.extension
        val renderer = extensions[extension]
                ?: extensions[filePath.extension] // fallback to a non-double extension
                ?: throw IllegalArgumentException("No Renderer registered for extension '${filePath.extension}'.")
        return renderer.render(filePath, model, ctx)
    }

    @JvmStatic
    fun register(fileRenderer: FileRenderer, vararg ext: String) = ext.forEach {
        if (extensions[it] != null) {
            Javalin.log?.info("'$it' is already registered to ${extensions[it]!!.javaClass}. Overriding.")
        }
        extensions[it] = fileRenderer
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
    private val String.doubleExtension: String get() = this.substringBeforeLast(".", "").extension + this.extension
    private val String.hasTwoDots: Boolean get() = this.count { it == '.' } > 1
}
