/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

open class Validator<T>(val value: T, val messagePrefix: String = "Value") {

    data class Rule<T>(val test: (T) -> Boolean, val invalidMessage: String)

    private val rules = mutableSetOf<Rule<T>>()

    @JvmOverloads
    open fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): Validator<T> {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this
    }

    fun get(): T = rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value

    companion object {
        @JvmStatic
        @JvmOverloads
        fun <T> create(clazz: Class<T>, value: String?, messagePrefix: String = "Value"): Validator<T> {
            if (value == null || value.isEmpty()) throw BadRequestResponse("$messagePrefix cannot be null or empty")
            return Validator(try {
                JavalinValidation.converters[clazz]?.invoke(value) ?: throw ConversionException(clazz.simpleName)
            } catch (e: Exception) {
                if (e is ConversionException) throw e
                throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
            } as T, messagePrefix)
        }
    }

}
