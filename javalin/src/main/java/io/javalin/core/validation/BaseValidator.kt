/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.validation

import io.javalin.core.util.JavalinLogger
import io.javalin.plugin.json.JsonMapper

open class BaseValidator<T>(
    val stringValue: String?,
    val clazz: Class<T>,
    fieldName: String,
    private val jsonMapper: JsonMapper? = null
) : TypedValueBaseValidator<T>(fieldName) {
    override fun validateValue(): Map<String, List<ValidationError<T>>> {
        if (this is BodyValidator) {
            try {
                typedValue = jsonMapper!!.fromJsonString(stringValue!!, clazz)
            } catch (e: Exception) {
                JavalinLogger.info("Couldn't deserialize body to ${clazz.simpleName}", e)
                return mapOf(REQUEST_BODY to listOf(ValidationError("DESERIALIZATION_FAILED", value = stringValue)))
            }
        } else if (this is NullableValidator || this is Validator) {
            try {
                typedValue = JavalinValidation.convertValue(clazz, stringValue)
            } catch (e: Exception) {
                JavalinLogger.info("Parameter '$fieldName' with value '$stringValue' is not a valid ${clazz.simpleName}")
                return mapOf(fieldName to listOf(ValidationError("TYPE_CONVERSION_FAILED", value = stringValue)))
            }
            if (this !is NullableValidator && typedValue == null) { // only check typedValue - null might map to 0, which could be valid?
                return mapOf(fieldName to listOf(ValidationError("NULLCHECK_FAILED", value = stringValue)))
            }
        }
        /** after this point [typedValue] replaces [stringValue] */
        return validateTypedValue()
    }
}
