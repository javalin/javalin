/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

class Validator(val value: String?, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule<String>>()

    @JvmOverloads
    fun check(predicate: (String) -> Boolean, errorMessage: String = "Failed check"): Validator {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this
    }

    fun matches(regex: String) = check({ Regex(regex).matches(it) }, "does not match '$regex'")

    /** Can be called for readability, but presence is asserted in [getOrThrow]. */
    fun notNullOrEmpty() = this

    fun getOrThrow(): String {
        if (value == null || value.isEmpty()) throw BadRequestResponse("$messagePrefix cannot be null or empty")
        return rules.validate(value)
    }

    inline fun <reified T : Any> asClass() = asClass(T::class.java)
    fun asBoolean() = asClass(Boolean::class.java)
    fun asDouble() = asClass(Double::class.java)
    fun asFloat() = asClass(Float::class.java)
    fun asInt() = asClass(Int::class.java)
    fun asLong() = asClass(Long::class.java)

    fun <T> asClass(clazz: Class<T>): TypedValidator<T> {
        val validValue = getOrThrow() // throw appropriate error messages before type conversion
        return TypedValidator(try {
            JavalinValidation.converters[clazz]?.invoke(validValue) ?: throw ConversionException(clazz.simpleName)
        } catch (e: Exception) {
            if (e is ConversionException) throw e
            throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
        } as T, messagePrefix)
    }

}

class TypedValidator<T>(val value: T, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule<T>>()

    @JvmOverloads
    fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): TypedValidator<T> {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this;
    }

    fun getOrThrow() = rules.validate(value)

}

private data class Rule<T>(val test: (T) -> Boolean, val invalidMessage: String)

/** Find first invalid [Rule] and throw, else return validated value */
private fun <T> Set<Rule<T>>.validate(value: T) = this.find { !it.test.invoke(value) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value
