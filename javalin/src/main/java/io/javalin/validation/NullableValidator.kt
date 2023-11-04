/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

/**
 * The NullableValidator is a [Validator] that allows null values, created by calling [Validator.allowNullable].
 */
open class NullableValidator<T>(typedValue: T? = null, params: Params<T>) : BaseValidator<T>(typedValue, params) {
    fun check(check: Check<T?>, error: String) = addRule(params.fieldName, check, error) as NullableValidator<T>
    fun check(check: Check<T?>, error: ValidationError<T>) = addRule(params.fieldName, check, error) as NullableValidator<T>
}
