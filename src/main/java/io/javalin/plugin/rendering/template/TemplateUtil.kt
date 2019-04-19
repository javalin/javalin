/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.rendering.template

object TemplateUtil {
    @JvmStatic
    fun model(vararg args: Any?): Map<String, Any?> {
        if (args.size % 2 != 0) {
            throw IllegalArgumentException("Number of arguments must be even (key value pairs).")
        }
        return args.asSequence().chunked(2).associate { it[0] as String to it[1] }
    }
}
