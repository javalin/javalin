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

    fun <T> convertValue(clazz: Class<T>, value: String?): T {
        val converter = converters[clazz] ?: throw MissingConverterException(clazz.simpleName)
        return (if (value != null) converter.invoke(value) else null) as T
    }

    @JvmStatic
    fun register(clazz: Class<*>, converter: (String) -> Any?) = converters.put(clazz, converter)

    @JvmStatic
    fun collectErrors(vararg validators: Validator<*>): Map<String, List<String>> {
        return collectErrors(validators.toList())
    }

    @JvmStatic
    fun collectErrors(validators: Iterable<Validator<*>>): Map<String, List<String>> {
        val allErrors = mutableMapOf<String, MutableList<String>>()
        validators.forEach { validator ->
            validator.errors().forEach { (fieldName, errorMessages) ->
                if (allErrors[fieldName] != null) {
                    allErrors[fieldName]?.addAll(errorMessages)
                } else {
                    allErrors[fieldName] = errorMessages.toMutableList()
                }
            }
        }
        return allErrors
    }
}

fun Iterable<Validator<*>>.collectErrors(): Map<String, List<String>> {
    return JavalinValidation.collectErrors(this)
}
