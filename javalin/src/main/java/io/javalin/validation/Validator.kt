/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.util.JavalinException
import io.javalin.util.javalinLazy

typealias Check<T> = (T) -> Boolean

data class Rule(val fieldName: String, val check: Check<Any?>, val error: ValidationError<Any?>)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : JavalinException("Validation failed")
data class ValidationError<out T> @JvmOverloads constructor(
    val message: String,
    val args: Map<String, Any?> = mapOf(),
    @JvmField var value: Any? = null,
    private val exception: Exception? = null
) {
    fun exception(): Exception? = exception
}

data class Params(
    val fieldName: String,
    val clazz: Class<*>? = null,
    val stringValue: String? = null,
    val typedValue: Any? = null,
    val valueSupplier: () -> Any? = { null }
)

@Suppress("UNCHECKED_CAST")
open class Validator<T> internal constructor(internal val params: Params, internal val fieldName: String = params.fieldName) {
    internal val rules = mutableListOf<Rule>()
    internal var typedValue: Any? = null

    protected open val conversionErrorMessage = "TYPE_CONVERSION_FAILED"

    private val errors: Map<String, List<ValidationError<Any?>>> by javalinLazy {
        typedValue = params.typedValue ?: try {
            params.valueSupplier()
        } catch (e: Exception) {
            return@javalinLazy mapOf(fieldName to listOf(ValidationError(conversionErrorMessage, value = params.stringValue, exception = e)))
        }
        val errors = mutableMapOf<String, MutableList<ValidationError<Any?>>>()
        rules.filter { !it.check(typedValue) }.forEach { failedRule ->
            errors.computeIfAbsent(failedRule.fieldName) { mutableListOf() }
            errors[failedRule.fieldName]!!.add(failedRule.error.also { it.value = typedValue })
        }
        errors.mapValues { it.value.toList() }.toMap()
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors as Map<String, List<ValidationError<T>>>
    fun hasValue() = !params.stringValue.isNullOrEmpty() || typedValue != null

    internal fun addCheck(fieldName: String, check: Check<Any?>, error: ValidationError<Any?>) {
        rules.add(Rule(fieldName, check, error))
    }

    open fun check(check: Check<T>, error: String) = apply { addCheck(fieldName, check as Check<Any?>, ValidationError(error)) }
    open fun check(check: Check<T>, error: ValidationError<*>) = apply { addCheck(fieldName, check as Check<Any?>, error) }

    open fun required(): Validator<T & Any> {
        @Suppress("UNCHECKED_CAST")
        return this as Validator<T & Any>
    }

    fun get(): T & Any {
        if (errors.isNotEmpty()) throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
        return typedValue as? (T & Any) ?: throw ValidationException(mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = params.stringValue))))
    }

    fun getOrNull(): T? {
        if (errors.isNotEmpty()) throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
        return typedValue as T?
    }

    fun getOrDefault(default: T & Any): T & Any = getOrNull() ?: default

    fun getOrThrow(exceptionFunction: (Map<String, List<ValidationError<Any>>>) -> Exception): T & Any {
        if (errors.isNotEmpty()) throw exceptionFunction(errors as Map<String, List<ValidationError<Any>>>)
        return typedValue as? (T & Any) ?: throw exceptionFunction(mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = params.stringValue))))
    }
}
