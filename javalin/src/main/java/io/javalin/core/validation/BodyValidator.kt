/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

open class BodyValidator<T>(value: T?, messagePrefix: String = "Value") : Validator<T>(value, messagePrefix) {

    private var errors: MutableMap<String, MutableList<String>>? = null

    @JvmOverloads
    open fun check(fieldName: String, predicate: (T) -> Boolean, errorMessage: String = "Failed check"): BodyValidator<T> {
        rules.add(Rule(fieldName, predicate, errorMessage))
        return this
    }

    fun isValid(): Boolean {
        if (errors == null) {
            validate()
        }
        return errors!!.isEmpty()
    }

    fun hasError(): Boolean {
        if (errors == null) {
            validate()
        }
        return errors!!.isNotEmpty()
    }

    fun errors(): Map<String, List<String>> {
        if (errors == null) {
            validate()
        }
        return errors!!
    }

    private fun validate(): BodyValidator<T> {
        errors = mutableMapOf()
        rules.forEach { rule ->
            if (value != null) {
                try {
                    if (!rule.test.invoke(value)) {
                        if (!errors!!.containsKey(rule.fieldName)) {
                            errors!![rule.fieldName] = mutableListOf()
                        }
                        errors!![rule.fieldName]?.add(rule.invalidMessage)
                    }
                } catch (ignore : NullPointerException) {
                }
            }
        }
        return this
    }
}
