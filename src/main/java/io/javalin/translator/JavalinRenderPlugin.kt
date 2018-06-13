/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator

import io.javalin.translator.markdown.JavalinCommonmarkPlugin
import io.javalin.translator.template.*

interface FileRenderer {
    fun render(filePath: String, model: Map<String, Any?>): String
}

object JavalinRenderingPlugin {

    private val extensions = mutableMapOf<String, FileRenderer>()

    init {
        register(".vm", JavalinVelocityPlugin)
        register(".vtl", JavalinVelocityPlugin)
        register(".ftl", JavalinFreemarkerPlugin)
        register(".mustache", JavalinMustachePlugin)
        register(".jtwig", JavalinJtwigPlugin)
        register(".peb", JavalinPebblePlugin)
        register(".pebble", JavalinPebblePlugin)
        register(".html", JavalinThymeleafPlugin)
        register(".thyme", JavalinThymeleafPlugin)
        register(".thymeleaf", JavalinThymeleafPlugin)
        register(".md", JavalinCommonmarkPlugin)
        register(".markdown", JavalinCommonmarkPlugin)
    }

    fun renderBasedOnExtension(filePath: String, model: Map<String, Any?>): String {
        return extensions[filePath.extension]!!.render(filePath, model)
    }

    fun register(extension: String, fileRenderer: FileRenderer) {
        extensions[extension] = fileRenderer;
    }

    private val String.extension: String get() = this.replaceBeforeLast(".", "")
}





