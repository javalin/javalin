/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

/**
 * The non-nullable [Validator] uses [NullableRule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
open class Validator<T>(val value: T?, val messagePrefix: String = "Value", val key: String = "Parameter") {

    val rules = mutableSetOf<NullableRule<T>>()

    fun allowNullable() = NullableValidator(value, messagePrefix, key)

    @JvmOverloads
    fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): Validator<T> {
        rules.add(NullableRule(key, { predicate(it!!) }, errorMessage))
        return this
    }

    fun get(): T = when {
        value == null -> throw BadRequestResponse("$messagePrefix cannot be null or empty")
        rules.allValid(value) -> value
        else -> throw BadRequestResponse("$messagePrefix invalid - ${rules.firstErrorMsg(value)}")
    }

    fun errors() = rules.getErrors(value)

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> create(clazz: Class<T>, value: String?, messagePrefix: String = "Value", key: String = "Parameter") = try {
            Validator(JavalinValidation.convertValue(clazz, value), messagePrefix, key)
        } catch (e: Exception) {
            if (e is MissingConverterException) throw e
            throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
        }
    }

}
