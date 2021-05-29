package io.javalin.core.validation

data class NullableRule<T>(val fieldName: String, val check: (T?) -> Boolean, val invalidMessage: String)

fun <T> Set<NullableRule<T>>.getErrors(value: T?): MutableMap<String, MutableList<String>> {
    val errors = mutableMapOf<String, MutableList<String>>()
    this.forEach { rule ->
        if (value != null && !rule.check(value)) {
            errors.computeIfAbsent(rule.fieldName) { mutableListOf() }
            errors[rule.fieldName]!!.add(rule.invalidMessage)
        }
    }
    return errors
}

fun <T> Set<NullableRule<T>>.allValid(value: T?) = this.all { it.check(value) }

fun <T> Set<NullableRule<T>>.firstErrorMsg(value: T?) = this.find { !it.check(value) }?.invalidMessage
