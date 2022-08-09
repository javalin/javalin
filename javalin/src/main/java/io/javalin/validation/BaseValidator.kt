/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.json.JsonMapper
import io.javalin.util.JavalinLogger

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: ValidationError<T>)
data class ValidationError<T>(val message: String, val args: Map<String, Any?> = mapOf(), var value: Any? = null)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : RuntimeException()

data class StringSource<T>(
    val stringValue: String?,
    val clazz: Class<T>,
    val jsonMapper: JsonMapper? = null
)

open class BaseValidator<T>(val fieldName: String, protected var typedValue: T?, protected val stringSource: StringSource<T>?) {
    internal val rules = mutableListOf<Rule<T>>()

    constructor(stringValue: String?, clazz: Class<T>, fieldName: String, jsonMapper: JsonMapper? = null) :
        this(fieldName, null, StringSource<T>(stringValue, clazz, jsonMapper))

    private val errors by lazy {
        if (stringSource != null) {
            if (this is BodyValidator) {
                try {
                    typedValue = stringSource.jsonMapper!!.fromJsonString(stringSource.stringValue!!, stringSource.clazz)
                } catch (e: Exception) {
                    JavalinLogger.info("Couldn't deserialize body to ${stringSource.clazz.simpleName}", e)
                    return@lazy mapOf(REQUEST_BODY to listOf(ValidationError("DESERIALIZATION_FAILED", value = stringSource.stringValue)))
                }
            } else if (this is NullableValidator || this is Validator) {
                try {
                    typedValue = JavalinValidation.convertValue(stringSource.clazz, stringSource.stringValue)
                } catch (e: Exception) {
                    JavalinLogger.info("Parameter '$fieldName' with value '${stringSource.stringValue}' is not a valid ${stringSource.clazz.simpleName}")
                    return@lazy mapOf(fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value = stringSource.stringValue)))
                }
                if (this !is NullableValidator && typedValue == null) { // only check typedValue - null might map to 0, which could be valid?
                    return@lazy mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = stringSource.stringValue)))
                }
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

    open fun get(): T? = getOrThrow { ValidationException(it) }

    open fun getOrThrow(exceptionFunction: (Map<String, List<ValidationError<Any>>>) -> Exception): T? = when {
        errors.isEmpty() -> typedValue
        else -> throw exceptionFunction(errors as Map<String, List<ValidationError<Any>>>)
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors
}
