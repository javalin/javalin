/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

class Validator @JvmOverloads constructor(val value: String?, private val messagePrefix: String = "Value") {

    data class Rule(val test: (String) -> Boolean, val invalidMessage: String)

    private val rules = mutableSetOf<Rule>()

    fun notNullOrBlank() = this // can be called for readability, but we always ensure that value is present

    fun check(predicate: (String) -> Boolean, errorMessage: String): Validator {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this;
    }

    fun matches(regex: String): Validator {
        rules.add(Rule({ Regex(regex).matches(it) }, "$messagePrefix does not match '$regex'"))
        return this
    }

    fun get(): String {
        if (value == null || value.isEmpty()) {
            throw BadRequestResponse("$messagePrefix cannot be null or blank")
        }
        rules.forEach { rule ->
            if (!rule.test.invoke(value)) {
                throw BadRequestResponse(rule.invalidMessage)
            }
        }
        return value
    }

    fun <T> getAs(clazz: Class<T>): T {
        val validValue = this.get()
        return try {
            JavalinValidation.converters[clazz]?.invoke(validValue) as T
                    ?: throw IllegalArgumentException("Can't auto-cast to ${clazz.simpleName}. Register a custom converter using JavalinValidation#register.")
        } catch (e: Exception) {
            throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
        }
    }

    inline fun <reified T : Any> getAs(): T = getAs(T::class.java)

}

