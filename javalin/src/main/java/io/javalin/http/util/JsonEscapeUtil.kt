/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.util

object JsonEscapeUtil {
    fun escape(str: String): String {
        val builder = StringBuilder(str.length)
        for (ch in str) {
            builder.append(when (ch) {
                '\"' -> "\\\""
                '\n' -> "\\n"
                '\r' -> "\\r"
                '\\' -> "\\\\"
                '\t' -> "\\t"
                '\b' -> "\\b"
                else -> ch
            })
        }
        return builder.toString()
    }
}
