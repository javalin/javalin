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

    // Convert to typed validator
    fun <T> asClass(clazz: Class<T>) = TypedValidator(convertToType(clazz, getOrThrow()), messagePrefix)

    inline fun <reified T : Any> asClass() = asClass(T::class.java)
    fun asBoolean() = asClass(Boolean::class.java)
    fun asDouble() = asClass(Double::class.java)
    fun asFloat() = asClass(Float::class.java)
    fun asInt() = asClass(Int::class.java)
    fun asLong() = asClass(Long::class.java)

    private fun <T> convertToType(clazz: Class<T>, value: String) = try {
        JavalinValidation.converters[clazz]?.invoke(value) ?: throw IllegalArgumentException("Can't convert to ${clazz.simpleName}. Register a converter using JavalinValidation#register.")
    } catch (e: Exception) {
        if (e.message?.startsWith("Can't convert to") == true) throw e
        throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
    } as T

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
