/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

open class BaseValidator<T>(val value: T?) {

    val rules = mutableSetOf<NullableRule<T>>()

    fun addRule(fieldName: String, predicate: (T?) -> Boolean, errorMessage: String): BaseValidator<T> {
        rules.add(NullableRule(fieldName, predicate, errorMessage))
        return this
    }

    open fun get(): T? = when {
        rules.allValid(value) -> value
        else -> throw BadRequestResponse(rules.firstErrorMsg(value) ?: ValidationError.CUSTOM_CHECK_FAILED.name)
    }

    fun errors() = rules.getErrors(value)

}
