/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.Javalin
import io.javalin.http.HttpStatus

class MissingConverterException(val className: String) : RuntimeException()

object JavalinValidation {

    private val converters = mutableMapOf<Class<*>, (String) -> Any?>(
        java.lang.Boolean::class.java to { it.toBoolean() },
        java.lang.Double::class.java to { it.toDouble() },
        java.lang.Float::class.java to { it.toFloat() },
        java.lang.Integer::class.java to { it.toInt() },
        java.lang.Long::class.java to { it.toLong() },
        java.lang.String::class.java to { it },
        Boolean::class.java to { it.toBoolean() },
        Double::class.java to { it.toDouble() },
        Float::class.java to { it.toFloat() },
        Int::class.java to { it.toInt() },
        Long::class.java to { it.toLong() },
        String::class.java to { it }
    )

    fun <T> convertValue(clazz: Class<T>, value: String?): T {
        val converter = converters[clazz] ?: throw MissingConverterException(clazz.name)
        return (if (value != null) converter.invoke(value) else null) as T
    }

    fun <T> hasConverter(clazz: Class<T>) = converters[clazz] != null

    @JvmStatic
    fun register(clazz: Class<*>, converter: (String) -> Any?) = converters.put(clazz, converter)

    @JvmStatic
    fun collectErrors(vararg validators: BaseValidator<*>) = collectErrors(validators.toList())

    @JvmStatic
    fun collectErrors(validators: Iterable<BaseValidator<*>>): Map<String, List<ValidationError<out Any?>>> =
        validators.flatMap { it.errors().entries }.associate { it.key to it.value }

    @JvmStatic
    fun addValidationExceptionMapper(app: Javalin) {
        app.exception(ValidationException::class.java) { e, ctx ->
            ctx.json(e.errors).status(HttpStatus.BAD_REQUEST)
        }
    }
}


fun Iterable<BaseValidator<*>>.collectErrors(): Map<String, List<ValidationError<out Any?>>> =
    JavalinValidation.collectErrors(this)
