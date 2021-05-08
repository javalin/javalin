/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering

import io.javalin.core.util.JavalinLogger
import io.javalin.http.Context
import io.javalin.plugin.rendering.markdown.JavalinCommonmark
import io.javalin.plugin.rendering.template.JavalinFreemarker
import io.javalin.plugin.rendering.template.JavalinJte
import io.javalin.plugin.rendering.template.JavalinJtwig
import io.javalin.plugin.rendering.template.JavalinMustache
import io.javalin.plugin.rendering.template.JavalinPebble
import io.javalin.plugin.rendering.template.JavalinThymeleaf
import io.javalin.plugin.rendering.template.JavalinVelocity

object JavalinRenderer {

    private val extensions = mutableMapOf<String, FileRenderer>()

    init {
        register(JavalinVelocity, ".vm", ".vtl")
        register(JavalinFreemarker, ".ftl")
        register(JavalinMustache, ".mustache")
        register(JavalinJtwig, ".jtwig", ".twig")
        register(JavalinPebble, ".peb", ".pebble")
        register(JavalinThymeleaf, ".html", ".tl", ".thyme", ".thymeleaf")
        register(JavalinCommonmark, ".md", ".markdown")
        register(JavalinJte, ".jte", ".kte")
    }

    @JvmField
    var baseModelFunction: (Context) -> Map<String, Any?> = { mapOf<String, Any>() }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        val renderer = extensions[filePath.extension] ?: throw IllegalArgumentException("No Renderer registered for extension '${filePath.extension}'.")
        return renderer.render(filePath, baseModelFunction(ctx) + model, ctx)//overrides the base model
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
