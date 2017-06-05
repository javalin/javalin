/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

object Thymeleaf {

    private var templateEngine: TemplateEngine? = null

    fun configure(staticTemplateEngine: TemplateEngine) {
        templateEngine = staticTemplateEngine
    }

    fun render(templatePath: String, model: Map<String, Any>): String {
        templateEngine = templateEngine ?: defaultThymeLeafEngine()
        val context = Context()
        context.setVariables(model)
        return templateEngine!!.process(templatePath, context)
    }

    fun defaultThymeLeafEngine() : TemplateEngine {
        val templateEngine = TemplateEngine()
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.templateMode = TemplateMode.HTML
        templateEngine.setTemplateResolver(templateResolver)
        return templateEngine;
    }

}
