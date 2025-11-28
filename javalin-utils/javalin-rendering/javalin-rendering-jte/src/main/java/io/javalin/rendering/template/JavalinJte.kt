/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.ResourceCodeResolver
import io.javalin.http.Context
import io.javalin.rendering.FileRenderer

class JavalinJte @JvmOverloads constructor(
    private var templateEngine: TemplateEngine = defaultTemplateEngine()
) : FileRenderer {

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        val output = StringOutput()
        templateEngine.render(filePath, model, output)
        return output.toString().trim()
    }

    companion object {
        fun defaultTemplateEngine(): TemplateEngine {
            val codeResolver = ResourceCodeResolver("")
            return TemplateEngine.create(codeResolver, ContentType.Html)
        }
    }

}

