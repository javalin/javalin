/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.http.InternalServerErrorResponse

/**
 * The non-nullable [Validator] uses [Rule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
open class Validator<T>(value: T?, fieldName: String) : BaseValidator<T>(value, fieldName) {

    fun allowNullable(): NullableValidator<T> {
        if (this.rules.isEmpty()) return NullableValidator(value, fieldName)
        throw IllegalStateException("Validator#allowNullable must be called before adding rules")
    }

    fun check(check: Check<T>, error: String) = addRule(fieldName, { check(it!!) }, error) as Validator<T>
    override fun get(): T = super.get()!!

    companion object {
        @JvmStatic
        fun <T> create(clazz: Class<T>, value: String?, fieldName: String) = try {
            Validator(JavalinValidation.convertValue(clazz, value), fieldName)
        } catch (e: Exception) {
            if (e is MissingConverterException) {
                JavalinLogger.info("Can't convert to ${e.className}. Register a converter using JavalinValidation#register.")
                throw InternalServerErrorResponse()
            }
            JavalinLogger.info("Parameter '${fieldName}' with value '${value}' is not a valid ${clazz.simpleName}")
            throw ValidationException(mapOf(fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value))))
        }
    }
}

