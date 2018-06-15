/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import io.javalin.core.util.Util
import io.javalin.translator.FileRenderer
import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter
import java.nio.charset.StandardCharsets

object JavalinVelocityPlugin : FileRenderer {

    private var velocityEngine: VelocityEngine? = null

    @JvmStatic
    fun configure(staticVelocityEngine: VelocityEngine) {
        velocityEngine = staticVelocityEngine
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("Apache Velocity", "org.apache.velocity.Template", "org.apache.velocity/velocity")
        velocityEngine = velocityEngine ?: defaultVelocityEngine()
        val stringWriter = StringWriter()
        velocityEngine!!.getTemplate(filePath, StandardCharsets.UTF_8.name()).merge(
                VelocityContext(model.toMutableMap()), stringWriter
        )
        return stringWriter.toString()
    }

    private fun defaultVelocityEngine(): VelocityEngine {
        val velocityEngine = VelocityEngine()
        velocityEngine.setProperty("resource.loader", "class")
        velocityEngine.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader")
        velocityEngine.setProperty("velocimacro.library.autoreload", "true")
        velocityEngine.setProperty("file.resource.loader.cache", "false")
        velocityEngine.setProperty("velocimacro.permissions.allow.inline.to.replace.global", "true")
        return velocityEngine
    }

}
