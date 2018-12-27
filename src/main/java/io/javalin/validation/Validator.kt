/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

open class TypedValidator<T>(val value: T?, val messagePrefix: String = "Value") {

    data class Rule<T>(val test: (T) -> Boolean, val invalidMessage: String)

    private val rules = mutableSetOf<Rule<T>>()

    @JvmOverloads
    open fun check(predicate: (T) -> Boolean, errorMessage: String = "Failed check"): TypedValidator<T> {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this
    }

    fun getOrThrow(): T {
        if (value == null || (value is String && value.isEmpty())) throw BadRequestResponse("$messagePrefix cannot be null or empty")
        return rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value
    }

}

class Validator(value: String?, messagePrefix: String = "Value") : TypedValidator<String>(value, messagePrefix) {

    override fun check(predicate: (String) -> Boolean, errorMessage: String) = super.check(predicate, errorMessage) as Validator

    fun matches(regex: String) = check({ Regex(regex).matches(it) }, "does not match '$regex'")

    fun notNullOrEmpty() = this // can be called for readability, is always checked in TypedValidator#getOrThrow

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
