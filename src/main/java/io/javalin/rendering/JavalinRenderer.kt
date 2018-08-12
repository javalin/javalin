/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering

import io.javalin.rendering.markdown.JavalinCommonmark
import io.javalin.rendering.template.*
import org.slf4j.LoggerFactory

object JavalinRenderer {

    private val log = LoggerFactory.getLogger(JavalinRenderer.javaClass)

    private val extensions = mutableMapOf<String, FileRenderer>()

    init {
        register(JavalinVelocity, ".vm", ".vtl")
        register(JavalinFreemarker, ".ftl")
        register(JavalinMustache, ".mustache")
        register(JavalinJtwig, ".jtwig", ".twig")
        register(JavalinPebble, ".peb", ".pebble")
        register(JavalinThymeleaf, ".html", ".tl", ".thyme", ".thymeleaf")
        register(JavalinCommonmark, ".md", ".markdown")
    }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>): String {
        val renderer = extensions[filePath.extension]
        if (renderer == null) {
            throw IllegalArgumentException("No Renderer registered for extension '${filePath.extension}'.")
        }
        return renderer.render(filePath, model)
    }

    @JvmStatic
    fun register(fileRenderer: FileRenderer, vararg ext: String) = ext.forEach {
        if (extensions[it] != null) {
            log.info("'$it' is already registered to ${extensions[it]!!.javaClass}. Overriding.")
        }
        extensions[it] = fileRenderer
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
}
