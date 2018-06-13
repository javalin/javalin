/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator

import io.javalin.translator.markdown.JavalinCommonmarkPlugin
import io.javalin.translator.template.*
import org.slf4j.LoggerFactory

interface FileRenderer {
    fun render(filePath: String, model: Map<String, Any?>): String
}

object JavalinRenderingPlugin {

    private val log = LoggerFactory.getLogger(JavalinRenderingPlugin.javaClass)

    private val extensions = mutableMapOf<String, FileRenderer>()

    init {
        register(JavalinVelocityPlugin, ".vm", ".vtl")
        register(JavalinFreemarkerPlugin, ".ftl")
        register(JavalinMustachePlugin, ".mustache")
        register(JavalinJtwigPlugin, ".jtwig")
        register(JavalinPebblePlugin, ".peb", ".pebble")
        register(JavalinThymeleafPlugin, ".html", ".tl", ".thyme", ".thymeleaf")
        register(JavalinCommonmarkPlugin, ".md", ".markdown")
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
            log.info("'${it}' is already registered to ${extensions[it]!!.javaClass}. Overriding.")
        }
        extensions[it] = fileRenderer
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
}





