/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse
import java.util.*

class Validator @JvmOverloads constructor(val value: String?, private val messagePrefix: String = "Value") {

    data class Rule(val test: (String) -> Boolean, val invalidMessage: String)

    private val notNullOrBlank = Rule({ it.isEmpty() }, "$messagePrefix cannot be null or blank")

    private val rules = mutableSetOf<Rule>()

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

    fun get(): String {
        if (value == null || value.isEmpty()) {
            throw BadRequestResponse(notNullOrBlank.invalidMessage)
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

object JavalinValidation {
    val converters = mutableMapOf<Class<*>, (String) -> Any>(
            Int::class.java to { s -> s.toInt() },
            Integer::class.java to { s -> s.toInt() },
            Double::class.java to { s -> s.toDouble() },
            Long::class.java to { s -> s.toLong() },
            Date::class.java to { s -> Date(s) }
    )

    @JvmStatic
    fun register(clazz: Class<*>, converter: (String) -> Any) = converters.put(clazz, converter)

    @JvmStatic
    fun validate(value: String?) = Validator(value)
}
