/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.rendering.template

import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.output.StringOutput
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.http.Context
import io.javalin.http.servlet.isLocalhost
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.util.RenderingDependency.JTE
import io.javalin.rendering.util.RenderingDependency.JTE_KOTLIN
import io.javalin.rendering.util.Util
import java.io.File

class JavalinJte @JvmOverloads constructor(
    private val templateEngine: TemplateEngine = defaultJteEngine(),
    private val isDevFunction: (Context) -> Boolean = { ctx -> ctx.isLocalhost() }
) : FileRenderer {

    init {
        Util.throwIfNotAvailable(JTE)
    }

    private var isDev: Boolean? = null // cached and easily accessible, is set on first request (can't be configured directly by end user)

    override fun render(filePath: String, model: Map<String, Any?>, context: Context): String {
        if (isDev == true && filePath.endsWith(".kte")) {
            Util.throwIfNotAvailable(JTE_KOTLIN)
        }
        isDev = isDev ?: isDevFunction(context)
        val stringOutput = StringOutput()
        templateEngine.render(filePath, model, stringOutput)
        return stringOutput.toString()
    }

    companion object {
        private fun defaultJteEngine() = TemplateEngine.create(DirectoryCodeResolver(File("src/main/jte").toPath()), ContentType.Html)
    }

}
