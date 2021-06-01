/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: String)
data class ValidationError<T>(val message: String, val value: T?)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : Exception()

open class BaseValidator<T>(val value: T?, val fieldName: String) {

    internal val rules = mutableListOf<Rule<T>>()
    private val errors by lazy {
        val errors = mutableMapOf<String, MutableList<ValidationError<T>>>()
        if (value == null && this !is NullableValidator) {
            errors[fieldName] = mutableListOf(ValidationError("NULLCHECK_FAILED", value))
        }
        rules.forEach { rule ->
            if (value != null && !rule.check(value)) {
                // the same validator can have multiple field names if it's a BodyValidator
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(ValidationError(rule.error, value))
            }
        }
        errors.mapValues { it.value.toList() }.toMap() // make immutable
    }

    fun addRule(fieldName: String, check: Check<T?>, error: String): BaseValidator<T> {
        rules.add(Rule(fieldName, check, error))
        return this
    }

    open fun get(): T? = when {
        errors.isEmpty() -> value
        else -> throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors

}
