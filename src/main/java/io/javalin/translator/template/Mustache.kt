/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.javalin.core.util.Util
import io.javalin.translator.FileRenderer
import java.io.StringWriter

object JavalinMustachePlugin : FileRenderer {

    private var mustacheFactory: MustacheFactory? = null

    @JvmStatic
    fun configure(staticMustacheFactory: MustacheFactory) {
        mustacheFactory = staticMustacheFactory
    }

    override fun render(filePath: String, model: Map<String, Any?>): String {
        Util.ensureDependencyPresent("Mustache", "com.github.mustachejava.Mustache", "com.github.spullara.mustache.java/compiler")
        mustacheFactory = mustacheFactory ?: DefaultMustacheFactory("./")
        val stringWriter = StringWriter()
        mustacheFactory!!.compile(filePath).execute(stringWriter, model).close()
        return stringWriter.toString()
    }

}
