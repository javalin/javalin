/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import freemarker.template.Configuration
import freemarker.template.TemplateException
import freemarker.template.Version
import java.io.IOException
import java.io.StringWriter

object Freemarker {

    private var configuration: Configuration? = null

    fun configure(staticConfiguration: Configuration) {
        configuration = staticConfiguration
    }

    fun render(templatePath: String, model: Map<String, Any>): String {
        configuration = configuration ?: defaultFreemarkerEngine();
        try {
            val stringWriter = StringWriter()
            configuration!!.getTemplate(templatePath).process(model, stringWriter)
            return stringWriter.toString()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun defaultFreemarkerEngine() : Configuration {
        val configuration = Configuration(Version(2, 3, 26))
        configuration.setClassForTemplateLoading(Freemarker::class.java, "/")
        return configuration;
    }

}
