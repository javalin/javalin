/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import io.javalin.http.Context
import io.javalin.rendering.FileRenderer
import java.io.StringWriter

class JavalinMustache @JvmOverloads constructor(
    private var mustacheFactory: MustacheFactory = defaultMustacheFactory()
) : FileRenderer {

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        val stringWriter = StringWriter()
        mustacheFactory.compile(filePath).execute(stringWriter, model).close()
        return stringWriter.toString()
    }

    companion object {
        fun defaultMustacheFactory(): MustacheFactory = DefaultMustacheFactory("./")
    }

}
