/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import org.jtwig.environment.DefaultEnvironmentConfiguration
import org.jtwig.environment.EnvironmentConfiguration

object JavalinJtwigPlugin {

    private var configuration: EnvironmentConfiguration? = null

    @JvmStatic
    fun configure(staticConfiguration: EnvironmentConfiguration) {
        JavalinJtwigPlugin.configuration = staticConfiguration
    }

    fun render(path: String, model: Map<String, Any?>): String {
        val configuration = configuration ?: DefaultEnvironmentConfiguration()
        val template = JtwigTemplate.classpathTemplate(path, configuration)
        return template.render(JtwigModel.newModel(model))
    }

}
