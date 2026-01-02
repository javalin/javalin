/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

const val REQUEST_BODY = "REQUEST_BODY"

@Suppress("UNCHECKED_CAST")
class BodyValidator<T> internal constructor(value: String, clazz: Class<*>, valueSupplier: () -> Any?) :
    Validator<T>(Params(REQUEST_BODY, clazz, value, valueSupplier = valueSupplier), REQUEST_BODY) {
    override val conversionErrorMessage = "DESERIALIZATION_FAILED"
    override fun check(check: Check<T>, error: String) = apply { addCheck(REQUEST_BODY, check as Check<Any?>, ValidationError(error)) }
    override fun check(check: Check<T>, error: ValidationError<*>) = apply { addCheck(REQUEST_BODY, check as Check<Any?>, error) }
    fun check(fieldName: String, check: Check<T>, error: String) = apply { addCheck(fieldName, check as Check<Any?>, ValidationError(error)) }
    fun check(fieldName: String, check: Check<T>, error: ValidationError<*>) = apply { addCheck(fieldName, check as Check<Any?>, error) }
    override fun required(): BodyValidator<T & Any> = this as BodyValidator<T & Any>
}
