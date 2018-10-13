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
    private val complexExtensions = mutableMapOf<String, FileRenderer>()

    init {
        register(JavalinVelocity, ".vm", ".vtl")
        register(JavalinFreemarker, ".ftl")
        register(JavalinMustache, ".mustache")
        register(JavalinJtwig, ".jtwig", ".twig")
        register(JavalinPebble, ".peb", ".pebble")
        register(JavalinThymeleaf, ".html", ".tl", ".thyme", ".thymeleaf")
        register(JavalinCommonmark, ".md", ".markdown")
        register(JavalinRocker, ".rocker.html")
    }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>): String {
        var complexExtensionRenderer: FileRenderer? = null
        if (filePath.hasAnyExtension && filePath.complexExtension.hasMultipleDots) {
            complexExtensionRenderer = complexExtensions[filePath.complexExtension]
        }
        val renderer = complexExtensionRenderer ?: extensions[filePath.extension]
        ?: throw IllegalArgumentException("No Renderer registered for extension '${filePath.extension}'.")
        return renderer.render(filePath, model)
    }

    @JvmStatic
    fun register(fileRenderer: FileRenderer, vararg ext: String) = ext.forEach {
        if (it.hasMultipleDots) {
            if (complexExtensions[it] != null) {
                log.info("'$it' is already registered to ${complexExtensions[it]!!.javaClass}. Overriding.")
            }
            complexExtensions[it] = fileRenderer
        } else {
            if (extensions[it] != null) {
                log.info("'$it' is already registered to ${extensions[it]!!.javaClass}. Overriding.")
            }
            extensions[it] = fileRenderer
        }
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
    private val String.hasAnyExtension: Boolean get() = this.indexOf(".") >= 0
    private val String.complexExtension: String get() = this.substring(this.indexOf("."))
    private val String.hasMultipleDots: Boolean get() = this.hasAnyExtension && this.indexOfFirst { it == '.' } != this.indexOfLast { it == '.' }
}
