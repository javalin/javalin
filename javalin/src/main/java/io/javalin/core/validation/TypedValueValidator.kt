package io.javalin.core.validation

import org.jetbrains.annotations.NotNull

/**
 * The non-nullable [TypedValueValidator] uses [Rule] rules, but checks if value is null before calling them.
 * The [check] method wraps its non-nullable predicate in a nullable predicate
 */
class TypedValueValidator<T>(typedVal: T?, fieldName: String) : TypedValueBaseValidator<T>(fieldName, typedVal) {

    fun allowNullable(): TypedValueNullableValidator<T> {
        if (rules.isEmpty()) return TypedValueNullableValidator(typedValue, fieldName)
        throw IllegalStateException("TypedValueValidator#allowNullable must be called before adding rules")
    }

    fun check(check: Check<T>, error: String) =
        addRule(fieldName, { check(it!!) }, error) as TypedValueValidator<T>

    fun check(check: Check<T>, error: ValidationError<T>) =
        addRule(fieldName, { check(it!!) }, error) as TypedValueValidator<T>

    @NotNull // there is a null-check in BaseValidator
    override fun get(): T = super.get()!!

    fun getOrDefault(default: T): T = when (typedValue) {
        null -> default
        else -> super.get()!!
    }

    companion object {
        @JvmStatic
        fun <T> create(typedValue: String?, fieldName: String) =
            TypedValueValidator(typedValue, fieldName)
    }
}
