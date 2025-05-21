/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import io.javalin.http.Context
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.util.RenderingDependency.PEBBLE
import io.javalin.rendering.util.Util
import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.loader.ClasspathLoader
import java.io.StringWriter

class JavalinPebble @JvmOverloads constructor(
    private var pebbleEngine: PebbleEngine = defaultPebbleEngine()
) : FileRenderer {

    init {
        Util.throwIfNotAvailable(PEBBLE)
    }

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        val compiledTemplate = pebbleEngine.getTemplate(filePath)
        val stringWriter = StringWriter()
        compiledTemplate.evaluate(stringWriter, model)
        return stringWriter.toString()
    }

    companion object {
        private fun defaultPebbleEngine() = PebbleEngine.Builder()
            .loader(ClasspathLoader())
            .strictVariables(false)
            .build()
    }

}
