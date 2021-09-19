/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.http.InternalServerErrorResponse
import org.jetbrains.annotations.NotNull

/**
 * The non-nullable [Validator] uses [Rule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
open class Validator<T>(
    fieldName: String,
    typedValue: T? = null,
    stringSource: StringSource<T>? = null
) : BaseValidator<T>(fieldName, typedValue, stringSource) {

    constructor(stringValue: String?, clazz: Class<T>, fieldName: String) :
            this(fieldName, null, StringSource<T>(stringValue, clazz))

    fun allowNullable(): NullableValidator<T> {
        if (this.rules.isEmpty()) return NullableValidator(fieldName, typedValue, stringSource)
        throw IllegalStateException("Validator#allowNullable must be called before adding rules")
    }

    fun check(check: Check<T>, error: String) = addRule(fieldName, { check(it!!) }, error) as Validator<T>
    fun check(check: Check<T>, error: ValidationError<T>) = addRule(fieldName, { check(it!!) }, error) as Validator<T>

    @NotNull // there is a null-check in BaseValidator
    override fun get(): T = super.get()!!

    fun getOrDefault(default: T): T {
        return if (stringSource?.stringValue != null || typedValue != null) super.get()!! else default
    }

    companion object {
        @JvmStatic
        fun <T> create(clazz: Class<T>, value: String?, fieldName: String) = if (JavalinValidation.hasConverter(clazz)) {
            Validator(value, clazz, fieldName)
        } else {
            JavalinLogger.info("Can't convert to ${clazz.simpleName}. Register a converter using JavalinValidation#register.")
            throw InternalServerErrorResponse()
        }
    }
}

