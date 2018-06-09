/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import java.io.StringWriter

object JavalinPebblePlugin {

    private var pebbleEngine: PebbleEngine? = null

    @JvmStatic
    fun configure(staticPebbleEngine: PebbleEngine) {
        pebbleEngine = staticPebbleEngine
    }

    fun render(templatePath: String, model: Map<String, Any?>): String {
        pebbleEngine = pebbleEngine ?: defaultPebbleEngine()
        val compiledTemplate = pebbleEngine!!.getTemplate(templatePath)
        val stringWriter = StringWriter()
        compiledTemplate.evaluate(stringWriter, model);
        return stringWriter.toString()
    }

    private fun defaultPebbleEngine(): PebbleEngine {
        return PebbleEngine.Builder()
                .loader(ClasspathLoader())
                .strictVariables(false)
                .build()
    }

}
