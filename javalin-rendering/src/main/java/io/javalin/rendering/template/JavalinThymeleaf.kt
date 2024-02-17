/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import io.javalin.http.Context
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.util.RenderingDependency.THYMELEAF
import io.javalin.rendering.util.Util
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.WebContext
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import org.thymeleaf.web.servlet.JakartaServletWebApplication

class JavalinThymeleaf @JvmOverloads constructor(
    private var templateEngine: TemplateEngine = defaultThymeLeafEngine()
) : FileRenderer {

    init {
        Util.throwIfNotAvailable(THYMELEAF)
    }

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        // ctx.req.servletContext that is passed to buildApplication has to match ctx.req.servletContext passed into
        // buildExchange. (application.servletContext === ctx.req.servletContext)
        val application = JakartaServletWebApplication.buildApplication(context.req().servletContext)
        val webExchange = application.buildExchange(context.req(), context.res())
        val webContext = WebContext(webExchange, webExchange.locale, model)
        return templateEngine.process(filePath, webContext)
    }

    companion object {
        private fun defaultThymeLeafEngine() = TemplateEngine().apply {
            setTemplateResolver(ClassLoaderTemplateResolver().apply {
                templateMode = TemplateMode.HTML
            })
        }
    }

}
