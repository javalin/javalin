/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse
import java.util.*

data class Rule(val predicate: (String) -> Boolean, val invalidMessage: String)

class Validator(val value: String?, private val messagePrefix: String = "Value") {

    private val rules = mutableSetOf<Rule>()

    private val notNullOrBlank = Rule({ it.isEmpty() }, "$messagePrefix cannot be null or blank")

    private fun addToRules(rule: Rule): Validator {
        rules.add(rule)
        return this;
    }

    fun notNullOrBlank() = addToRules(notNullOrBlank) // i think we'll always check this... include for readability?

    fun check(predicate: (String) -> Boolean, errorMessage: String) = addToRules(
            Rule(predicate, "$messagePrefix invalid - $errorMessage")
    )

    fun matches(regex: String) = addToRules(
            Rule({ Regex(regex).matches(it) }, "$messagePrefix does not match '$regex'")
    )

    @Suppress("UNCHECKED_CAST")
    fun get(): String {
        if (value == null || value.isEmpty()) {
            throw BadRequestResponse(notNullOrBlank.invalidMessage)
        }
        rules.forEach { rule ->
            if (!rule.predicate.invoke(value)) {
                throw BadRequestResponse(rule.invalidMessage)
            }
        }
        return value;
    }

    inline fun <reified T : Any> getAs(noinline converter: ((String) -> Any)? = null): T = getAs(T::class.java, converter)

    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <T> getAs(clazz: Class<T>, converter: ((String) -> Any)? = null): T {
        if (value == null || value.isEmpty()) {
            throw BadRequestResponse(notNullOrBlank.invalidMessage)
        }
        rules.forEach { rule ->
            if (!rule.predicate.invoke(value)) {
                throw BadRequestResponse(rule.invalidMessage)
            }
        }
        return if (converter != null) {
            convert(clazz) { converter.invoke(value) } as T
        } else when (clazz) {
            Int::class.java -> convert(clazz) { value.toInt() } as T
            Integer::class.java -> convert(clazz) { value.toInt() } as T
            Double::class.java -> convert(clazz) { value.toDouble() } as T
            Long::class.java -> convert(clazz) { value.toLong() } as T
            Date::class.java -> convert(clazz) { Date(value) } as T
            else -> throw IllegalArgumentException("Can't auto-cast to $clazz. Use get() and do it manually.")
        }
    }

    private fun convert(clazz: Class<*>, converter: () -> Any): Any = try {
        converter.invoke()
    } catch (e: Exception) {
        throw BadRequestResponse("$messagePrefix is not a valid ${clazz.simpleName}")
    }

}
