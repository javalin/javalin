/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class NullableValidator<T>(value: T?, fieldName: String) : BaseValidator<T>(value, fieldName) {
    fun check(check: Check<T?>, error: String) = addRule(fieldName, check, error) as NullableValidator<T>
    fun check(check: Check<T?>, error: ValidationError<T>) = addRule(fieldName, check, error) as NullableValidator<T>
}
