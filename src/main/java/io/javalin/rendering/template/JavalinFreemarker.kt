/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import freemarker.template.Configuration
import freemarker.template.Version
import io.javalin.core.util.Util
import io.javalin.rendering.FileRenderer
import java.io.StringWriter

object JavalinFreemarker : FileRenderer {

    private var configuration: Configuration? = null

    @JvmStatic
    fun configure(staticConfiguration: Configuration) {
        configuration = staticConfiguration
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("Apache Freemarker", "freemarker.template.Configuration", "org.freemarker/freemarker")
        configuration = configuration ?: defaultFreemarkerEngine()
        val stringWriter = StringWriter()
        configuration!!.getTemplate(filePath).process(model, stringWriter)
        return stringWriter.toString()
    }

    private fun defaultFreemarkerEngine(): Configuration {
        val configuration = Configuration(Version(2, 3, 26))
        configuration.setClassForTemplateLoading(JavalinFreemarker::class.java, "/")
        return configuration
    }

}
