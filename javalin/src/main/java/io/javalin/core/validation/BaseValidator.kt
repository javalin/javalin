/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.plugin.json.JsonMapper

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: ValidationError<T>)
data class ValidationError<T>(val message: String, val args: Map<String, Any?> = mapOf(), var value: Any? = null)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : RuntimeException()

open class BaseValidator<T>(val stringValue: String?, val clazz: Class<T>, val fieldName: String, jsonMapper: JsonMapper? = null) {
    private var typedValue: T? = null
    internal val rules = mutableListOf<Rule<T>>()
    private val errors by lazy {
        if (this is BodyValidator) {
            try {
                typedValue = jsonMapper!!.fromJsonString(stringValue!!, clazz)
            } catch (e: Exception) {
                JavalinLogger.info("Couldn't deserialize body to ${clazz.simpleName}", e)
                return@lazy mapOf(REQUEST_BODY to listOf(ValidationError("DESERIALIZATION_FAILED", value = stringValue)))
            }
        } else if (this is NullableValidator || this is Validator) {
            try {
                typedValue = JavalinValidation.convertValue(clazz, stringValue)
            } catch (e: Exception) {
                JavalinLogger.info("Parameter '${fieldName}' with value '${stringValue}' is not a valid ${clazz.simpleName}")
                return@lazy mapOf(fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value = stringValue)))
            }
            if (this !is NullableValidator && typedValue == null) { // only check typedValue - null might map to 0, which could be valid?
                return@lazy mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = stringValue)))
            }
        }
        /** after this point [typedValue] replaces [stringValue] */
        val errors = mutableMapOf<String, MutableList<ValidationError<T>>>()
        rules.filter { !it.check(typedValue) }.forEach { failedRule ->
            // if it's a BodyValidator, the same validator can have rules with different field names
            errors.computeIfAbsent(failedRule.fieldName) { mutableListOf() }
            errors[failedRule.fieldName]!!.add(failedRule.error.also { it.value = typedValue })
        }
        errors.mapValues { it.value.toList() }.toMap() // make immutable
    }

    protected fun addRule(fieldName: String, check: Check<T?>, error: String): BaseValidator<T> {
        rules.add(Rule(fieldName, check, ValidationError(error)))
        return this
    }

    protected fun addRule(fieldName: String, check: Check<T?>, error: ValidationError<T>): BaseValidator<T> {
        rules.add(Rule(fieldName, check, error))
        return this
    }

    open fun get(): T? = when {
        errors.isEmpty() -> typedValue
        else -> throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors

}
