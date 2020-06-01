/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class BodyValidator<T>(value: T?, messagePrefix: String = "Value") : Validator<T>(value, messagePrefix) {
    @JvmOverloads
    open fun check(fieldName: String, predicate: (T) -> Boolean, errorMessage: String = "Failed check"): BodyValidator<T> {
        rules.add(Rule(fieldName, predicate, errorMessage))
        return this
    }
}
