/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.config.JavalinConfig
import io.javalin.config.Key
import io.javalin.config.ValidationConfig
import io.javalin.http.HttpStatus
import io.javalin.util.JavalinLogger

class MissingConverterException(val className: String) : RuntimeException()

class Validation(private val validationConfig: ValidationConfig = ValidationConfig()) {

    private fun <T> convertValue(clazz: Class<T>, value: String?): T {
        val converter = validationConfig.converters[clazz] ?: throw MissingConverterException(clazz.name)
        @Suppress("UNCHECKED_CAST")
        return (if (value != null) converter.invoke(value) else null) as T
    }

    private fun <T> supportsClass(clazz: Class<T>) = validationConfig.converters[clazz] != null

    fun <T> validator(fieldName: String, clazz: Class<T>, value: String?): Validator<T> {
        if (!supportsClass(clazz)) {
            JavalinLogger.info("Can't convert to ${clazz.name}. Register a converter using config.validation.registerConverter().")
            throw MissingConverterException(clazz.name)
        }
        return Validator(Params(fieldName, clazz, value) { convertValue(clazz, value) })
    }

    fun <T> validator(fieldName: String, typedValue: T?): Validator<T> =
         Validator(Params(fieldName, null, null, typedValue) { null })

    companion object {
        @JvmField
        val ValidationKey = Key<Validation>("javalin-validation")

        @JvmStatic
        fun collectErrors(vararg validators: BaseValidator<*>) = collectErrors(validators.toList())

        @JvmStatic
        fun collectErrors(validators: Iterable<BaseValidator<*>>): Map<String, List<ValidationError<out Any?>>> =
            validators.flatMap { it.errors().entries }.associate { it.key to it.value }

        @JvmStatic
        fun addValidationExceptionMapper(cfg: JavalinConfig) {
            cfg.pvt.internalRouter.addHttpExceptionHandler(ValidationException::class.java) { e, ctx ->
                ctx.json(e.errors).status(HttpStatus.BAD_REQUEST)
            }
        }
    }

}

fun Iterable<BaseValidator<*>>.collectErrors(): Map<String, List<ValidationError<out Any?>>> =
    Validation.collectErrors(this)
