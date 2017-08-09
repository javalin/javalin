/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import org.apache.velocity.VelocityContext
import org.apache.velocity.app.VelocityEngine
import java.io.StringWriter
import java.nio.charset.StandardCharsets

object JavalinVelocityPlugin {

    private var velocityEngine: VelocityEngine? = null

    @JvmStatic
    fun configure(staticVelocityEngine: VelocityEngine) {
        velocityEngine = staticVelocityEngine
    }

    fun render(templatePath: String, model: Map<String, Any>): String {
        velocityEngine = velocityEngine ?: defaultVelocityEngine()
        val stringWriter = StringWriter()
        velocityEngine!!.getTemplate(templatePath, StandardCharsets.UTF_8.name()).merge(
                VelocityContext(model), stringWriter
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
