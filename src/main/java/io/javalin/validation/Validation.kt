/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.validation

import io.javalin.BadRequestResponse
import io.javalin.Context

enum class Param {
    QUERY, FORM, PATH
}

data class Rule(val predicate: (String) -> Boolean, val invalidMessage: String)

class Validator(paramType: Param, key: String, ctx: Context) {

    private val rules = mutableSetOf<Rule>()

    private val value = when (paramType) {
        Param.QUERY -> ctx.queryParam(key)
        Param.FORM -> ctx.formParam(key)
        Param.PATH -> ctx.pathParam(key)
    }

    private val param = when (paramType) {
        Param.QUERY -> "Query parameter '$key'"
        Param.FORM -> "Form parameter '$key'"
        Param.PATH -> "Path parameter '$key'"
    }

    private val notNullOrBlank = Rule({ v -> !v.isEmpty() }, "$param cannot be null or blank")

    private fun addToRules(rule: Rule): Validator {
        rules.add(rule)
        return this;
    }

    fun notNullOrBlank() = addToRules(notNullOrBlank) // i think we'll always check this... include for readability?

    fun check(predicate: (String) -> Boolean, errorMessage: String) = addToRules(
            Rule(predicate, "$param invalid - $errorMessage")
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

    inline fun <reified T : Any> getAs(): T = getAs(T::class.java)

    @Suppress("UNCHECKED_CAST")
    fun <T> getAs(clazz: Class<T>): T {
        if (value == null || value.isEmpty()) {
            throw BadRequestResponse(notNullOrBlank.invalidMessage)
        }
        rules.forEach { rule ->
            if (!rule.predicate.invoke(value)) {
                throw BadRequestResponse(rule.invalidMessage)
            }
        }
        return when (clazz) {
            Int::class.java -> convert(Int::class.java) { value.toInt() } as T
            Integer::class.java -> convert(Integer::class.java) { value.toInt() } as T
            Double::class.java -> convert(Double::class.java) { value.toDouble() } as T
            else -> throw IllegalArgumentException("Can't auto-cast to $clazz. Use get() and do it manually.")
        }
    }

    private fun convert(clazz: Class<*>, converter: () -> Any): Any = try {
        converter.invoke()
    } catch (e: Exception) {
        throw BadRequestResponse("$param is not a valid ${clazz.simpleName}")
    }

}
