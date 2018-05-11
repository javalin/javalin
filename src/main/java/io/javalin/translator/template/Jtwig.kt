/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import org.jtwig.JtwigModel
import org.jtwig.JtwigTemplate

object JavalinJtwigPlugin {

    fun render(path: String, model: Map<String, Any?>): String {
        val template = JtwigTemplate.classpathTemplate(path)
        return template.render(JtwigModel.newModel(model))
    }

}
