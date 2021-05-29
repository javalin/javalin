/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

open class NullableValidator<T>(val value: T?, val messagePrefix: String = "Value", val key: String = "Parameter") {

    val rules = mutableSetOf<NullableRule<T>>()

    @JvmOverloads
    fun check(predicate: (T?) -> Boolean, errorMessage: String = "Failed check"): NullableValidator<T> {
        rules.add(NullableRule(key, predicate, errorMessage))
        return this
    }

    fun get(): T? {
        val failedRuled = rules.find { !it.check(value) }
        return if (failedRuled == null) value else throw BadRequestResponse("$messagePrefix invalid - ${failedRuled.invalidMessage}")
    }

    fun errors(): MutableMap<String, MutableList<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        rules.forEach { rule ->
            if (value != null && !rule.check(value)) {
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(rule.invalidMessage)
            }
        }
        return errors
    }

}
