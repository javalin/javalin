/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import io.javalin.core.util.Util
import io.javalin.rendering.FileRenderer
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver

object JavalinThymeleafPlugin : FileRenderer {

    private var templateEngine: TemplateEngine? = null

    @JvmStatic
    fun configure(staticTemplateEngine: TemplateEngine) {
        templateEngine = staticTemplateEngine
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("Thymeleaf", "org.thymeleaf.TemplateEngine", "org.thymeleaf/thymeleaf-spring3")
        templateEngine = templateEngine ?: defaultThymeLeafEngine()
        val context = Context()
        context.setVariables(model)
        return templateEngine!!.process(filePath, context)
    }

    private fun defaultThymeLeafEngine(): TemplateEngine {
        val templateEngine = TemplateEngine()
        val templateResolver = ClassLoaderTemplateResolver()
        templateResolver.templateMode = TemplateMode.HTML
        templateEngine.setTemplateResolver(templateResolver)
        return templateEngine
    }

}
