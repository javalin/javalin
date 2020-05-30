/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.http.BadRequestResponse

open class Validator<T>(val value: T?, val messagePrefix: String = "Value", val key: String = "Parameter") {

    data class Rule<T>(val fieldName: String, val test: (T) -> Boolean, val invalidMessage: String)

    protected open var errors: MutableMap<String, MutableList<String>>? = null

    protected val rules = mutableSetOf<Rule<T>>()

    @JvmOverloads
    open fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): Validator<T> {
        rules.add(Rule(key, predicate, errorMessage))
        return this
    }

    //These two options will fail fast but only provide the first failure.
    fun get(): T = getOrNull() ?: throw BadRequestResponse("$messagePrefix cannot be null or empty")

    fun getOrNull(): T? {
        if (value == null) return null
        return rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse("$messagePrefix invalid - ${it.invalidMessage}") } ?: value
    }

    open fun isValid(): Boolean {
        if (errors == null) {
            validate()
        }
        return errors!!.isEmpty()
    }

    open fun hasError(): Boolean {
        if (errors == null) {
            validate()
        }
        return errors!!.isNotEmpty()
    }

    open fun errors(): Map<String, List<String>> {
        if (errors == null) {
            validate()
        }
        return errors!!
    }

    protected open fun validate() {
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
                } catch (ignore: NullPointerException) {
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> create(clazz: Class<T>, value: String?, messagePrefix: String = "Value", key: String = "Parameter"): Validator<T> {
            return Validator(try {
                val converter = JavalinValidation.converters[clazz] ?: throw MissingConverterException(clazz.simpleName)
                if (value != null && value.isNotEmpty()) {
                    converter.invoke(value) ?: throw NullPointerException()
                } else {
                    null
                }
            } catch (e: Exception) {
                if (e is MissingConverterException) throw e
                throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
            } as T, messagePrefix, key)
        }

        @JvmStatic
        fun collectErrors(vararg validators: Validator<*>): Map<String, List<String>> {
            return collectErrors(validators.toList())
        }

        @JvmStatic
        fun collectErrors(validators: Iterable<Validator<*>>): Map<String, List<String>> {
            val allErrors = mutableMapOf<String, MutableList<String>>()
            validators.forEach { validator ->
                validator.errors().forEach { (fieldName, errorMessages) ->
                    if (allErrors[fieldName] != null) {
                        allErrors[fieldName]?.addAll(errorMessages)
                    } else {
                        allErrors[fieldName] = errorMessages.toMutableList()
                    }
                }
            }
            return allErrors
        }
    }
}

fun Iterable<Validator<*>>.collectErrors(): Map<String, List<String>> {
    return Validator.collectErrors(this)
}
