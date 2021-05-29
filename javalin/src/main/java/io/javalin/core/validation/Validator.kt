/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

data class Rule<T>(val fieldName: String, val test: (T) -> Boolean, val invalidMessage: String)

open class Validator<T>(val value: T?, val messagePrefix: String = "Value", val key: String = "Parameter") {

    val rules = mutableSetOf<Rule<T>>()

    @JvmOverloads
    fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): Validator<T> {
        rules.add(Rule(key, predicate, errorMessage))
        return this
    }

    //These two options will fail fast but only provide the first failure.
    fun get(): T = getOrNull() ?: throw BadRequestResponse("$messagePrefix cannot be null or empty")

    fun getOrNull(): T? {
        if (value == null) return null
        return rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse("$messagePrefix invalid - ${it.invalidMessage}") } ?: value
    }

    fun errors(): MutableMap<String, MutableList<String>> {
        val errors = mutableMapOf<String, MutableList<String>>()
        rules.forEach { rule ->
            if (value != null) {
                if (!rule.test.invoke(value)) {
                    if (rule.fieldName !in errors.keys) {
                        errors[rule.fieldName] = mutableListOf()
                    }
                    errors[rule.fieldName]?.add(rule.invalidMessage)
                }
            }
        }
        return errors
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> create(clazz: Class<T>, value: String?, messagePrefix: String = "Value", key: String = "Parameter"): Validator<T> {
            return Validator(
                try {
                    val converter = JavalinValidation.converters[clazz] ?: throw MissingConverterException(clazz.simpleName)
                    if (value != null) {
                        converter.invoke(value) ?: throw NullPointerException()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    if (e is MissingConverterException) throw e
                    throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
                } as T, messagePrefix, key
            )
        }

    }
}
