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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object JavalinJtwig : FileRenderer {

    private var configuration: EnvironmentConfiguration? = null

    @JvmStatic
    fun configure(staticConfiguration: EnvironmentConfiguration) {
        configuration = staticConfiguration
    }

    var lock = ReentrantLock()

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.JTWIG)
        val configuration = configuration ?: lock.withLock {
            configuration ?: DefaultEnvironmentConfiguration()
        }
        val template = JtwigTemplate.classpathTemplate(filePath, configuration)
        return template.render(JtwigModel.newModel(model))
    }

}
