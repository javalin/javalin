package io.javalin.core.validation

open class TypedValueNullableValidator<T>(typedVal: T?, fieldName: String) : TypedValueBaseValidator<T>(fieldName, typedVal) {
    fun check(check: Check<T?>, error: String) =
        addRule(fieldName, check, error) as TypedValueNullableValidator<T>

    fun check(check: Check<T?>, error: ValidationError<T>) =
        addRule(fieldName, check, error) as TypedValueNullableValidator<T>
}
