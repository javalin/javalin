/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import org.jetbrains.annotations.NotNull

/**
 * The non-nullable [Validator] uses [Rule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
open class Validator<T> internal constructor(params: Params<T>) : BaseValidator<T>(params) {

    fun allowNullable(): NullableValidator<T> {
        if (this.rules.isEmpty()) return NullableValidator(params)
        throw IllegalStateException("Validator#allowNullable must be called before adding rules")
    }

    fun check(check: Check<T>, error: String) = addRule(params.fieldName, { check(it!!) }, error) as Validator<T>
    fun check(check: Check<T>, error: ValidationError<T>) = addRule(params.fieldName, { check(it!!) }, error) as Validator<T>

    fun hasValue() = !params.stringValue.isNullOrEmpty() || typedValue != null

    @NotNull // there is a null-check in BaseValidator
    override fun get(): T = super.get()!!

    fun getOrDefault(default: T) = if (hasValue()) super.get()!! else default

    @NotNull
    override fun getOrThrow(exceptionFunction: (Map<String, List<ValidationError<Any>>>) -> Exception) = super.getOrThrow(exceptionFunction)!!

}
