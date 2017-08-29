/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import java.io.StringWriter

object JavalinMustachePlugin {

    private var mustacheFactory: MustacheFactory? = null

    @JvmStatic
    fun configure(staticMustacheFactory: MustacheFactory) {
        mustacheFactory = staticMustacheFactory
    }

    fun render(templatePath: String, model: Map<String, Any>): String {
        mustacheFactory = mustacheFactory ?: DefaultMustacheFactory("./")
        val stringWriter = StringWriter()
        mustacheFactory!!.compile(templatePath).execute(stringWriter, model).close()
        return stringWriter.toString()
    }

}
