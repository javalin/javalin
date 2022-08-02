/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

open class NullableValidator<T>(fieldName: String, typedValue: T? = null, stringSource: StringSource<T>? = null) : BaseValidator<T>(fieldName, typedValue, stringSource) {
    constructor(stringValue: String?, clazz: Class<T>, fieldName: String) :
        this(fieldName, null, StringSource<T>(stringValue, clazz))

    fun check(check: Check<T?>, error: String) = addRule(fieldName, check, error) as NullableValidator<T>
    fun check(check: Check<T?>, error: ValidationError<T>) = addRule(fieldName, check, error) as NullableValidator<T>
}
