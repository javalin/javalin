/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class NullableValidator<T>(value: T?, val fieldName: String) : BaseValidator<T>(value) {
    fun check(check: Check<T?>, errorProvider: ErrorProvider) =
        addRule(fieldName, check, errorProvider) as NullableValidator<T>
}
