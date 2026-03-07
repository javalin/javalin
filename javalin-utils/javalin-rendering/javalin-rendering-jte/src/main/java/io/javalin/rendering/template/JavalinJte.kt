/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.DirectoryCodeResolver
import gg.jte.resolve.ResourceCodeResolver
import io.javalin.http.Context
import io.javalin.rendering.FileRenderer
import java.io.File

class JavalinJte @JvmOverloads constructor(
    private var templateEngine: TemplateEngine = classPathTemplateEngine()
) : FileRenderer {

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        val output = StringOutput()
        templateEngine.render(filePath, model, output)
        return output.toString().trim()
    }

    companion object {
        @Deprecated("Use classPathTemplateEngine() instead", ReplaceWith("classPathTemplateEngine()"))
        fun defaultTemplateEngine(): TemplateEngine = classPathTemplateEngine()

        fun classPathTemplateEngine(): TemplateEngine {
            val codeResolver = ResourceCodeResolver("")
            return TemplateEngine.create(codeResolver, ContentType.Html)
        }

        @JvmOverloads
        fun directoryTemplateEngine(path: String = "src/main/jte"): TemplateEngine {
            val codeResolver = DirectoryCodeResolver(File(path).toPath())
            return TemplateEngine.create(codeResolver, ContentType.Html)
        }
    }


}

