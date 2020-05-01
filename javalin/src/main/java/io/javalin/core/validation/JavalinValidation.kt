/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

class MissingConverterException(className: String) : IllegalArgumentException("Can't convert to $className. Register a converter using JavalinValidation#register.")

object JavalinValidation {
    val converters = mutableMapOf<Class<*>, (String) -> Any?>(
            java.lang.Boolean::class.java to { s -> s.toBoolean() },
            java.lang.Double::class.java to { s -> s.toDouble() },
            java.lang.Float::class.java to { s -> s.toFloat() },
            java.lang.Integer::class.java to { s -> s.toInt() },
            java.lang.Long::class.java to { s -> s.toLong() },
            java.lang.String::class.java to { s -> s },
            Boolean::class.java to { s -> s.toBoolean() },
            Double::class.java to { s -> s.toDouble() },
            Float::class.java to { s -> s.toFloat() },
            Int::class.java to { s -> s.toInt() },
            Long::class.java to { s -> s.toLong() },
            String::class.java to { s -> s }
    )

    @JvmStatic
    fun register(clazz: Class<*>, converter: (String) -> Any?) = converters.put(clazz, converter)
}
