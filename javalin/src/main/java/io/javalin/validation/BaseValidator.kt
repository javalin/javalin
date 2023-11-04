/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.util.JavalinLogger
import io.javalin.util.javalinLazy

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: ValidationError<T>)
data class ValidationError<T> @JvmOverloads constructor(val message: String, val args: Map<String, Any?> = mapOf(), var value: Any? = null)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : RuntimeException()

data class Params<T>(
    val fieldName: String,
    val clazz: Class<T>? = null,
    val stringValue: String? = null,
    val typedValue: T? = null,
    val valueSupplier: () -> T? = { null }
)

open class BaseValidator<T> internal constructor(protected val params: Params<T>) {
    internal val rules = mutableListOf<Rule<T>>()
    internal var typedValue: T? = null
    private val errors by javalinLazy {
        typedValue = params.typedValue ?: try {
            params.valueSupplier()
        } catch (e: Exception) {
            if (this is BodyValidator) {
                JavalinLogger.info("Couldn't deserialize body to ${params.clazz?.simpleName}", e)
                return@javalinLazy mapOf(REQUEST_BODY to listOf(ValidationError("DESERIALIZATION_FAILED", value = params.stringValue)))
            } else {
                JavalinLogger.info("Couldn't convert param '${params.fieldName}' with value '${params.stringValue}' to ${params.clazz?.simpleName}")
                return@javalinLazy mapOf(params.fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value = params.stringValue)))
            }
        }

        if (typedValue == null && this !is NullableValidator) { // only check typedValue - null might map to 0, which could be valid?
            return@javalinLazy mapOf(params.fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = params.stringValue)))
        }

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
