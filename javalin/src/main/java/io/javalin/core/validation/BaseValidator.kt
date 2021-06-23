/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.plugin.json.JavalinJson

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: ValidationError<T>)
data class ValidationError<T>(val message: String, val args: Map<String, Any?> = mapOf(), var value: Any? = null)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : Exception()

open class BaseValidator<T>(val value: String?, val clazz: Class<T>, val fieldName: String) {

    private var typedValue: T? = null

    internal val rules = mutableListOf<Rule<T>>()
    private val errors by lazy {
        if (this is BodyValidator) {
            try {
                typedValue = JavalinJson.fromJson(value!!, clazz)
            } catch (e: Exception) {
                JavalinLogger.info("Couldn't deserialize body to ${clazz.simpleName}", e)
                return@lazy mapOf(clazz.simpleName to listOf(ValidationError("DESERIALIZATION_FAILED", value = value)))
            }
        } else if (this is NullableValidator || this is Validator) {
            try {
                typedValue = JavalinValidation.convertValue(clazz, value)
            } catch (e: Exception) {
                JavalinLogger.info("Parameter '${fieldName}' with value '${value}' is not a valid ${clazz.simpleName}")
                return@lazy mapOf(fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value = value)))
            }
            if (this !is NullableValidator && typedValue == null) {
                return@lazy mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = value)))
            }
        }
        val errors = mutableMapOf<String, MutableList<ValidationError<T>>>()
        rules.forEach { rule ->
            if (value != null && !rule.check(typedValue)) {
                // the same validator can have multiple field names if it's a BodyValidator
                errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
                errors[rule.fieldName]!!.add(rule.error.also { it.value = typedValue })
            }
        }
        errors.mapValues { it.value.toList() }.toMap() // make immutable
    }

    fun addRule(fieldName: String, check: Check<T?>, error: String): BaseValidator<T> {
        rules.add(Rule(fieldName, check, ValidationError(error)))
        return this
    }

    fun addRule(fieldName: String, check: Check<T?>, error: ValidationError<T>): BaseValidator<T> {
        rules.add(Rule(fieldName, check, error))
        return this
    }

    open fun get(): T? = when {
        errors.isEmpty() -> typedValue
        else -> throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors

}
