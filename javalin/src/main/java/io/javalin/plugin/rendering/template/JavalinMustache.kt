/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.plugin.rendering.FileRenderer
import java.io.StringWriter

object JavalinMustache : FileRenderer {

    private var mustacheFactory: MustacheFactory? = null
    private val defaultMustacheFactory: MustacheFactory by lazy { DefaultMustacheFactory("./") }

    @JvmStatic
    fun configure(staticMustacheFactory: MustacheFactory) {
        mustacheFactory = staticMustacheFactory
    }

    override fun render(filePath: String, model: Map<String, Any?>, ctx: Context): String {
        Util.ensureDependencyPresent(OptionalDependency.MUSTACHE)
        val stringWriter = StringWriter()
        (mustacheFactory ?: defaultMustacheFactory).compile(filePath).execute(stringWriter, model).close()
        return stringWriter.toString()
    }

}
