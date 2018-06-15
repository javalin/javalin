/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.loader.ClasspathLoader
import io.javalin.core.util.Util
import io.javalin.translator.FileRenderer
import java.io.StringWriter

object JavalinPebblePlugin : FileRenderer {

    private var pebbleEngine: PebbleEngine? = null

    @JvmStatic
    fun configure(staticPebbleEngine: PebbleEngine) {
        pebbleEngine = staticPebbleEngine
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("pebble", "com.mitchellbosecke.pebble.PebbleEngine", "com.mitchellbosecke/pebble")
        pebbleEngine = pebbleEngine ?: defaultPebbleEngine()
        val compiledTemplate = pebbleEngine!!.getTemplate(filePath)
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
