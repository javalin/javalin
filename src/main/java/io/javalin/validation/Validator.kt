/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

class Validator @JvmOverloads constructor(val value: String?, private val messagePrefix: String = "Value") {

    init {
        if (value == null || value.isEmpty()) throw BadRequestResponse("$messagePrefix cannot be null or empty")
    }

    data class Rule(val test: (String) -> Boolean, val invalidMessage: String)

    private val rules = mutableSetOf<Rule>()

    fun check(predicate: (String) -> Boolean, errorMessage: String): Validator {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this;
    }

    fun matches(regex: String): Validator {
        rules.add(Rule({ Regex(regex).matches(it) }, "$messagePrefix does not match '$regex'"))
        return this
    }

    fun notNullOrEmpty() = this // can be called for readability, but presence is asserted in constructor

    // find first invalid rule and throw, else return validated value
    fun get(): String = rules.find { !it.test.invoke(value!!) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value!!

    fun <T> getAs(clazz: Class<T>) = convertToType(clazz, this.get())

    inline fun <reified T : Any> getAs(): T = getAs(T::class.java)

    private fun <T> convertToType(clazz: Class<T>, value: String) = try {
        JavalinValidation.converters[clazz]?.invoke(value) ?: throw IllegalArgumentException("Can't cast to ${clazz.simpleName}. Register a custom converter using JavalinValidation#register.")
    } catch (e: Exception) {
        throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
    } as T

}
