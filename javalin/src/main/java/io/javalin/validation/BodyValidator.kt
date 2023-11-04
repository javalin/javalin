/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import org.jetbrains.annotations.NotNull

const val REQUEST_BODY = "REQUEST_BODY"

open class BodyValidator<T> internal constructor(value: String, clazz: Class<T>, valueSupplier: () -> T?) : BaseValidator<T>(Params("", clazz, value, valueSupplier = valueSupplier)) {
    fun check(check: Check<T>, error: String) = check(fieldName = REQUEST_BODY, check, error)
    fun check(check: Check<T>, error: ValidationError<T>) = check(fieldName = REQUEST_BODY, check, error)
    fun check(fieldName: String, check: Check<T>, error: String) = addRule(fieldName, { check(it!!) }, error) as BodyValidator<T>
    fun check(fieldName: String, check: Check<T>, error: ValidationError<T>) = addRule(fieldName, { check(it!!) }, error) as BodyValidator<T>

    @NotNull // there is a null-check in BaseValidator
    override fun get(): T = super.get()!!

    @NotNull
    override fun getOrThrow(exceptionFunction: (Map<String, List<ValidationError<Any>>>) -> Exception): T = super.getOrThrow(exceptionFunction)!!
}
