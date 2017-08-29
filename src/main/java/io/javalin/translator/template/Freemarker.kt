/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import freemarker.template.Configuration
import freemarker.template.Version
import java.io.StringWriter

object JavalinFreemarkerPlugin {

    private var configuration: Configuration? = null

    @JvmStatic
    fun configure(staticConfiguration: Configuration) {
        configuration = staticConfiguration
    }

    fun render(templatePath: String, model: Map<String, Any>): String {
        configuration = configuration ?: defaultFreemarkerEngine()
        val stringWriter = StringWriter()
        configuration!!.getTemplate(templatePath).process(model, stringWriter)
        return stringWriter.toString()
    }

    private fun defaultFreemarkerEngine(): Configuration {
        val configuration = Configuration(Version(2, 3, 26))
        configuration.setClassForTemplateLoading(JavalinFreemarkerPlugin::class.java, "/")
        return configuration
    }

}
