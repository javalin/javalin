/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate
import org.jtwig.environment.DefaultEnvironmentConfiguration
import org.jtwig.environment.EnvironmentConfiguration

object JavalinJtwig : FileRenderer {

    private var configuration: EnvironmentConfiguration? = null
    private val defaultConfiguration: EnvironmentConfiguration by lazy { DefaultEnvironmentConfiguration() }

    @JvmStatic
    fun configure(staticConfiguration: EnvironmentConfiguration) {
        configuration = staticConfiguration
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.JTWIG)
        val template = JtwigTemplate.classpathTemplate(filePath, configuration ?: defaultConfiguration)
        return template.render(JtwigModel.newModel(model))
    }

}
