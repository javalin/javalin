/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

internal data class Rule<T>(val test: (T) -> Boolean, val invalidMessage: String)

class Validator(val value: String?, private val messagePrefix: String = "Value") {

    init {
        if (value == null || value.isEmpty()) throw BadRequestResponse("$messagePrefix cannot be null or empty")
    }

    private val rules = mutableSetOf<Rule<String>>()

    fun check(predicate: (String) -> Boolean, errorMessage: String = "Failed check"): Validator {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this;
    }

    fun matches(regex: String) = check({ Regex(regex).matches(it) }, "$messagePrefix does not match '$regex'")

    fun notNullOrEmpty() = this // can be called for readability, but presence is asserted in constructor

    fun getOrThrow() = validate(rules, value!!) // !! is safe, value is checked for null in init{}

    // Convert to typed validator

    fun asBoolean() = TypedValidator(convertToType(Boolean::class.java, getOrThrow()), messagePrefix)
    fun asDouble() = TypedValidator(convertToType(Double::class.java, getOrThrow()), messagePrefix)
    fun asFloat() = TypedValidator(convertToType(Float::class.java, getOrThrow()), messagePrefix)
    fun asInt() = TypedValidator(convertToType(Int::class.java, getOrThrow()), messagePrefix)
    fun asLong() = TypedValidator(convertToType(Long::class.java, getOrThrow()), messagePrefix)
    fun <T> asClass(clazz: Class<T>) = TypedValidator(convertToType(clazz, getOrThrow()), messagePrefix)
    inline fun <reified T : Any> asClass() = asClass(T::class.java)

    private fun <T> convertToType(clazz: Class<T>, value: String) = try {
        JavalinValidation.converters[clazz]?.invoke(value) ?: throw IllegalArgumentException("Can't convert to ${clazz.simpleName}. Register a converter using JavalinValidation#register.")
    } catch (e: Exception) {
        throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
    } as T

}

class TypedValidator<T>(val value: T, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule<T>>()

    fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): TypedValidator<T> {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this;
    }

    fun getOrThrow() = validate(rules, value)

}

// find first invalid rule and throw, else return validated value
private fun <T> validate(rules: Set<Rule<T>>, value: T) = rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value
