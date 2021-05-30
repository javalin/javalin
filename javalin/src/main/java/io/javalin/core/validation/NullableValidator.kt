/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

open class NullableValidator<T>(val value: T?, val fieldName: String, val fieldDescription: String) {

    val rules = mutableSetOf<NullableRule<T>>()

    fun check(predicate: (T?) -> Boolean, errorMessage: String): NullableValidator<T> {
        rules.add(NullableRule(fieldName, predicate, errorMessage))
        return this
    }

    fun get(): T? = when {
        rules.allValid(value) -> value
        else -> throw BadRequestResponse("$fieldDescription invalid - ${rules.firstErrorMsg(value)}")
    }

    fun errors() = rules.getErrors(value)

}
