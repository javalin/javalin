/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.translator.template

import java.util.*

object TemplateUtil {
    fun model(vararg args: Any): Map<String, Any> {
        if (args.size % 2 != 0) {
            throw RuntimeException("Number of arguments must be even (key value pairs)")
        }
        val model = HashMap<String, Any>()
        var i = 0
        while (i < args.size) {
            if (args[i] is String) {
                model.put(args[i] as String, args[i + 1])
            } else {
                throw RuntimeException("Keys must be Strings")
            }
            i += 2
        }
        return model
    }
}
