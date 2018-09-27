/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse

internal data class Rule<T>(val test: (T?) -> Boolean, val invalidMessage: String)

class Validator(val value: String?, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule<String>>()

    fun check(predicate: (String?) -> Boolean, errorMessage: String = "Failed check"): Validator {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this
    }

    fun matches(regex: String) = check({ if(it != null) Regex(regex).matches(it) else false }, "does not match '$regex'")

    // can be called for readability, also nice to as a fist check so !! can be used in next checks
    fun notNullOrEmpty() = check({it != null && !it.isEmpty()}, "cannot be null or empty")

    fun getOrThrow(): String {
        val valid: String? = validate(rules, value)
        if(valid != null) return valid else throw BadRequestResponse("$messagePrefix invalid - cannot be null or empty")
    }

    fun getOrDefault(default: String) : String{
        return validate(rules, value) ?: default
    }

    // Convert to typed validator

    fun asBoolean() = TypedValidator(convertToType(Boolean::class.java, validate(rules, value)), messagePrefix)
    fun asDouble() = TypedValidator(convertToType(Double::class.java, validate(rules, value)), messagePrefix)
    fun asFloat() = TypedValidator(convertToType(Float::class.java, validate(rules, value)), messagePrefix)
    fun asInt() = TypedValidator(convertToType(Int::class.java, validate(rules, value)), messagePrefix)
    fun asLong() = TypedValidator(convertToType(Long::class.java, validate(rules, value)), messagePrefix)
    fun <T> asClass(clazz: Class<T>) = TypedValidator(convertToType(clazz, validate(rules, value)), messagePrefix)
    inline fun <reified T : Any> asClass() = asClass(T::class.java)

    private fun <T> convertToType(clazz: Class<T>, value: String?): T? {
        if(value == null){
            return null
        }
        return try {
            JavalinValidation.converters[clazz]?.invoke(value) ?: throw IllegalArgumentException("Can't convert to ${clazz.simpleName}. Register a converter using JavalinValidation#register.")
        } catch (e: Exception) {
            throw BadRequestResponse("$messagePrefix invalid - not a valid ${clazz.simpleName}")
        } as T
    }
}

class TypedValidator<T>(val value: T?, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule<T>>()

    fun check(predicate: (T?) -> Boolean, errorMessage: String = "Failed check"): TypedValidator<T> {
        rules.add(Rule(predicate, "$messagePrefix invalid - $errorMessage"))
        return this
    }

    fun getOrThrow(): T {
        val valid: T? = validate(rules, value)
        if(valid != null) return valid else throw BadRequestResponse("$messagePrefix invalid - cannot be null or empty")
    }

    fun getOrDefault(default: T) : T{
        return validate(rules, value) ?: default
    }
}

// find first invalid rule and throw, else return validated value
private fun <T> validate(rules: Set<Rule<T>>, value: T?) = rules.find { !it.test.invoke(value) }?.let { throw BadRequestResponse(it.invalidMessage) } ?: value
