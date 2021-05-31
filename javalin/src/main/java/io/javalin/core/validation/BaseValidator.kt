/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse
import io.javalin.plugin.json.JavalinJson

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: String)
enum class RuleViolation { NULLCHECK_FAILED, TYPE_CONVERSION_FAILED, DESERIALIZATION_FAILED }

open class BaseValidator<T>(val value: T?, val fieldName: String) {

    internal val rules = mutableSetOf<Rule<T>>()
    private val errors by lazy {
        val errors = mutableMapOf<String, MutableList<String>>()
        if (value == null && this !is NullableValidator) {
            errors[fieldName] = mutableListOf(RuleViolation.NULLCHECK_FAILED.name)
        }
        rules.forEach { rule ->
            if (value != null && !rule.check(value)) {
                // the same validator can have multiple field names if it's a BodyValidator
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(rule.error)
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
        else -> throw BadRequestResponse(JavalinJson.toJson(errors))
    }

    fun errors() = errors

}
