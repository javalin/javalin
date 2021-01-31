/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import java.io.File

object JavalinJte : FileRenderer {

    private var templateEngine: TemplateEngine? = null
    private val defaultTemplateEngine: TemplateEngine by lazy { defaultJteEngine() }

    @JvmStatic
    fun configure(staticTemplateEngine: TemplateEngine) {
        templateEngine = staticTemplateEngine
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.JTE)

        val stringOutput = StringOutput()
        (templateEngine ?: defaultTemplateEngine).render(filePath, model, stringOutput)
        return stringOutput.toString()
    }

    private fun defaultJteEngine() = TemplateEngine.create(DirectoryCodeResolver(File("src/main/jte").toPath()), ContentType.Html)

}
