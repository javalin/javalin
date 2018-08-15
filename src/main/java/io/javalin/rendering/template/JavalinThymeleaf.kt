/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.rendering.FileRenderer
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

object JavalinThymeleaf : FileRenderer {

    private var templateEngine: TemplateEngine? = null

    @JvmStatic
    fun configure(staticTemplateEngine: TemplateEngine) {
        templateEngine = staticTemplateEngine
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent(OptionalDependency.THYMELEAF)
        templateEngine = templateEngine ?: defaultThymeLeafEngine()
        val context = Context()
        context.setVariables(model)
        return templateEngine!!.process(filePath, context)
    }

    private fun defaultThymeLeafEngine() = TemplateEngine().apply {
        setTemplateResolver(ClassLoaderTemplateResolver().apply {
            templateMode = TemplateMode.HTML
        })
    }

}
