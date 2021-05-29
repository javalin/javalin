package io.javalin.core.validation

data class NullableRule<T>(val fieldName: String, val check: (T?) -> Boolean, val invalidMessage: String)
data class Rule<T>(val fieldName: String, val check: (T) -> Boolean, val invalidMessage: String)
