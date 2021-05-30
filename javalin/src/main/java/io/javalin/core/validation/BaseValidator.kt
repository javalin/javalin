/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

typealias Check<T> = (T) -> Boolean
typealias ErrorProvider = () -> String

data class Rule<T>(val fieldName: String, val check: Check<T?>, val errorProvider: ErrorProvider)

open class BaseValidator<T>(val value: T?) {

    private val rules = mutableSetOf<Rule<T>>()

    fun addRule(fieldName: String, check: Check<T?>, errorProvider: ErrorProvider): BaseValidator<T> {
        rules.add(Rule(fieldName, check, errorProvider))
        return this
    }

    open fun get(): T? = when {
        rules.all { it.check(value) } -> value // all rules valid
        else -> throw BadRequestResponse(rules.firstErrorMsg(value) ?: ValidationError.CUSTOM_CHECK_FAILED.name)
    }

    fun errors(): MutableMap<String, MutableList<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        rules.forEach { rule ->
            if (value != null && !rule.check(value)) {
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(rule.errorProvider())
            }
        }
        return errors
    }

}

private fun <T> Set<Rule<T>>.firstErrorMsg(value: T?) = this.find { !it.check(value) }?.errorProvider?.invoke()
