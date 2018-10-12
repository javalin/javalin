/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import com.fizzed.rocker.Rocker
import com.fizzed.rocker.runtime.StringBuilderOutput
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.rendering.FileRenderer
import org.slf4j.LoggerFactory

object JavalinRocker : FileRenderer {
    private val log = LoggerFactory.getLogger(JavalinRocker.javaClass)
    override fun render(filePath: String?, model: MutableMap<String, Any>?): String {
        Util.ensureDependencyPresent(OptionalDependency.ROCKER)
        val template = Rocker.template(filePath).bind(model)
        val res = template.render()
        return res.toString()
    }

}
