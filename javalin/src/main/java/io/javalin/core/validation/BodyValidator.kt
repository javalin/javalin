/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class BodyValidator<T>(value: T?) : BaseValidator<T>(value) {

    fun check(check: Check<T>, errorProvider: ErrorProvider) = check("FIELD", check, errorProvider)

    fun check(fieldName: String, check: Check<T>, errorProvider: ErrorProvider) =
        addRule(fieldName, { check(it!!) }, errorProvider) as BodyValidator<T>

    override fun get(): T = super.get()!!
}
