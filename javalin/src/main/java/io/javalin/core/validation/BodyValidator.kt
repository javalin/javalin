/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.plugin.json.JsonMapper

open class BodyValidator<T>(stringValue: String?, clazz: Class<T>, jsonMapper: JsonMapper) : BaseValidator<T>(stringValue, clazz, clazz.simpleName, jsonMapper) {
    fun check(check: Check<T>, error: String) = check(clazz.simpleName, check, error)
    fun check(check: Check<T>, error: ValidationError<T>) = check(clazz.simpleName, check, error)
    fun check(fieldName: String, check: Check<T>, error: String) = addRule(fieldName, { check(it!!) }, error) as BodyValidator<T>
    fun check(fieldName: String, check: Check<T>, error: ValidationError<T>) = addRule(fieldName, { check(it!!) }, error) as BodyValidator<T>
    override fun get(): T = super.get()!!
}
