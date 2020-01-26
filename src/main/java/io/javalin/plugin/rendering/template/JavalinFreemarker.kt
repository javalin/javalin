/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

import freemarker.template.Configuration
import freemarker.template.Version
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import java.io.StringWriter

object JavalinFreemarker : FileRenderer {

    private var configuredConfiguration: Configuration? = null
    private val configuration: Configuration by lazy {
        configuredConfiguration ?: defaultFreemarkerEngine()
    }

    @JvmStatic
    fun configure(staticConfiguration: Configuration) {
        configuredConfiguration = staticConfiguration
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.FREEMARKER)
        val stringWriter = StringWriter()
        configuration.getTemplate(filePath).process(model, stringWriter)
        return stringWriter.toString()
    }

    private fun defaultFreemarkerEngine() = Configuration(Version(2, 3, 26)).apply {
        setClassForTemplateLoading(JavalinFreemarker::class.java, "/")
    }

}
