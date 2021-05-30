/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class BodyValidator<T>(value: T?, fieldDescription: String) : Validator<T>(value, "UNUSED", fieldDescription) {
    open fun check(fieldName: String, predicate: (T) -> Boolean, errorMessage: String): BodyValidator<T> {
        rules.add(NullableRule(fieldName, { predicate(it!!) }, errorMessage))
        return this
    }
}
