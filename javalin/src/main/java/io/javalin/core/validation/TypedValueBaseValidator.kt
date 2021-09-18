package io.javalin.core.validation

typealias Check<T> = (T) -> Boolean

data class Rule<T>(val fieldName: String, val check: Check<T?>, val error: ValidationError<T>)
data class ValidationError<T>(val message: String, val args: Map<String, Any?> = mapOf(), var value: Any? = null)
class ValidationException(val errors: Map<String, List<ValidationError<Any>>>) : RuntimeException()

open class TypedValueBaseValidator<T>(val fieldName: String, protected var typedValue: T? = null) {
    internal val rules = mutableListOf<Rule<T>>()

    private val errors by lazy {
        validateValue()
    }

    protected fun addRule(fieldName: String, check: Check<T?>, error: String): TypedValueBaseValidator<T> {
        rules.add(Rule(fieldName, check, ValidationError(error)))
        return this
    }

    protected fun addRule(fieldName: String, check: Check<T?>, error: ValidationError<T>): TypedValueBaseValidator<T> {
        rules.add(Rule(fieldName, check, error))
        return this
    }

    protected fun validateTypedValue(): Map<String, List<ValidationError<T>>> {
        val errors = mutableMapOf<String, MutableList<ValidationError<T>>>()
        rules.filter { !it.check(typedValue) }.forEach { failedRule ->
            // if it's a BodyValidator, the same validator can have rules with different field names
            errors.computeIfAbsent(failedRule.fieldName) { mutableListOf() }
            errors[failedRule.fieldName]!!.add(failedRule.error.also { it.value = typedValue })
        }
        return errors.mapValues { it.value.toList() }.toMap() // make immutable
    }

    protected open fun validateValue(): Map<String, List<ValidationError<T>>> = validateTypedValue()

    open fun get(): T? = when {
        errors.isEmpty() -> typedValue
        else -> throw ValidationException(errors as Map<String, List<ValidationError<Any>>>)
    }

    fun errors(): Map<String, List<ValidationError<T>>> = errors
}
