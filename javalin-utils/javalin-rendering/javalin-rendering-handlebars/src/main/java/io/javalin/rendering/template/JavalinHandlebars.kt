/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import io.javalin.http.Context
import io.javalin.rendering.FileRenderer

class JavalinHandlebars @JvmOverloads constructor(
    private var handlebars: Handlebars = defaultHandlebars()
) : FileRenderer {

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        val template = handlebars.compile(filePath)
        return template.apply(model)
    }

    companion object {
        fun defaultHandlebars(): Handlebars = Handlebars(ClassPathTemplateLoader("/", ""))
    }

}
