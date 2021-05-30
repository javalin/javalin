/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.http.BadRequestResponse
import io.javalin.http.InternalServerErrorResponse

enum class ValidationError { NULLCHECK_FAILED, TYPE_CONVERSION_FAILED, CUSTOM_CHECK_FAILED, DESERIALIZATION_FAILED }

/**
 * The non-nullable [Validator] uses [NullableRule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
open class Validator<T>(val value: T?, val fieldName: String) {

    val rules = mutableSetOf<NullableRule<T>>()

    fun allowNullable() = NullableValidator(value, fieldName)

    fun check(predicate: (T) -> Boolean, errorMessage: String): Validator<T> {
        rules.add(NullableRule(fieldName, { predicate(it!!) }, errorMessage))
        return this
    }

    fun get(): T = when {
        value == null -> {
            JavalinLogger.info("Parameter '${fieldName}' cannot be null")
            throw BadRequestResponse(ValidationError.NULLCHECK_FAILED.name)
        }
        rules.allValid(value) -> value
        else -> throw BadRequestResponse(rules.firstErrorMsg(value) ?: ValidationError.CUSTOM_CHECK_FAILED.name)
    }

    fun errors() = rules.getErrors(value)

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
            throw BadRequestResponse(ValidationError.TYPE_CONVERSION_FAILED.name)
        }
    }

}
