/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: String)
enum class RuleViolation { NULLCHECK_FAILED, TYPE_CONVERSION_FAILED, CUSTOM_CHECK_FAILED, DESERIALIZATION_FAILED }

open class BaseValidator<T>(val value: T?) {

    private val rules = mutableSetOf<Rule<T>>()

    fun addRule(fieldName: String, check: Check<T?>, error: String): BaseValidator<T> {
        rules.add(Rule(fieldName, check, error))
        return this
    }

    open fun get(): T? = when {
        rules.all { it.check(value) } -> value // all rules valid
        else -> throw BadRequestResponse(rules.firstErrorMsg(value) ?: RuleViolation.CUSTOM_CHECK_FAILED.name)
    }

    fun errors(): MutableMap<String, MutableList<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        rules.forEach { rule ->
            if (value != null && !rule.check(value)) {
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(rule.error)
            }
        }
        return errors
    }

}

private fun <T> Set<Rule<T>>.firstErrorMsg(value: T?) = this.find { !it.check(value) }?.error
