/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

object JavalinValidation {
    val converters = mutableMapOf<Class<*>, (String) -> Any>(
            java.lang.Boolean::class.java to { s -> s.toBoolean() },
            java.lang.Double::class.java to { s -> s.toDouble() },
            java.lang.Float::class.java to { s -> s.toFloat() },
            java.lang.Integer::class.java to { s -> s.toInt() },
            java.lang.Long::class.java to { s -> s.toLong() },
            Boolean::class.java to { s -> s.toBoolean() },
            Double::class.java to { s -> s.toDouble() },
            Float::class.java to { s -> s.toFloat() },
            Int::class.java to { s -> s.toInt() },
            Long::class.java to { s -> s.toLong() }
    )

    @JvmStatic
    fun register(clazz: Class<*>, converter: (String) -> Any) = converters.put(clazz, converter)

    @JvmStatic
    @JvmOverloads
    fun validate(value: String?, messagePrefix: String = "Value") = Validator(value, messagePrefix)
}
