/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import io.javalin.core.util.Util
import io.javalin.translator.FileRenderer
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import org.jtwig.environment.DefaultEnvironmentConfiguration
import org.jtwig.environment.EnvironmentConfiguration

object JavalinJtwigPlugin : FileRenderer {

    private var configuration: EnvironmentConfiguration? = null

    @JvmStatic
    fun configure(staticConfiguration: EnvironmentConfiguration) {
        JavalinJtwigPlugin.configuration = staticConfiguration
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("jTwig", "org.jtwig.JtwigTemplate", "org.jtwig/jtwig-core")
        val configuration = configuration ?: DefaultEnvironmentConfiguration()
        val template = JtwigTemplate.classpathTemplate(filePath, configuration)
        return template.render(JtwigModel.newModel(model))
    }

}
