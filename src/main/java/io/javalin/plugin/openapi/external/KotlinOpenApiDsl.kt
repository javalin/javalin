package io.javalin.plugin.openapi.external

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// The original functions are in https://github.com/derveloper/kotlin-openapi3-dsl/blob/master/src/main/kotlin/cc/vileda/openapi/dsl/OpenApiDsl.kt
// I needed to create a copy of all the inline functions, as it is not possible to call kotlin inline function from java code

fun <T> Components.schema(clazz: Class<T>, init: Schema<*>.() -> Unit) {
    if (schemas == null) {
        schemas = mutableMapOf()
    }
    val schema = findSchema(clazz)
    schema!!.init()
    schemas.put(clazz.simpleName, schema)
}

fun <T> Components.schema(clazz: Class<T>) {
    if (schemas == null) {
        schemas = mutableMapOf()
    }
    val schema = findSchema(clazz)
    schemas.put(clazz.simpleName, schema)
}

fun <T> Parameter.schema(clazz: Class<T>, init: Schema<*>.() -> Unit) {
    schema = findSchema(clazz)
    schema.init()
}

fun <T> Parameter.schema(clazz: Class<T>) {
    schema = findSchema(clazz)
}

fun <T> mediaType(clazz: Class<T>): MediaType {
    val mediaType = MediaType()
    val modelSchema = findSchema(clazz)
    mediaType.schema = modelSchema
    return mediaType
}

/**
 * This helper is not in the original library.
 * Because it is not possible to create a list instance of an unknown type we need a separate method for that.
 */
fun mediaTypeOfList(): MediaType {
    val mediaType = MediaType()
    val modelSchema = ArraySchema()
    mediaType.schema = modelSchema
    return mediaType
}

fun <T> mediaTypeRef(clazz: Class<T>): MediaType {
    val mediaType = MediaType()
    mediaType.schema = Schema<T>()
    mediaType.schema.`$ref` = clazz.simpleName
    return mediaType
}

fun <T> Content.mediaTypeRef(clazz: Class<T>, name: String) {
    val mediaType = mediaTypeRef(clazz)
    addMediaType(name, mediaType)
}

fun <T> Content.mediaType(clazz: Class<T>, name: String) {
    val mediaType = mediaType(clazz)
    addMediaType(name, mediaType)
}

fun <T> Content.mediaTypeArrayOfRef(clazz: Class<T>, name: String) {
    val mediaTypeArray = mediaTypeOfList()
    val mediaTypeObj = mediaTypeRef(clazz)
    (mediaTypeArray.schema as ArraySchema).items(mediaTypeObj.schema)
    addMediaType(name, mediaTypeArray)
}

fun <T> Content.mediaTypeArrayOf(clazz: Class<T>, name: String) {
    val mediaTypeArray = mediaTypeOfList()
    val mediaTypeObj = mediaType(clazz)
    (mediaTypeArray.schema as ArraySchema).items(mediaTypeObj.schema)
    addMediaType(name, mediaTypeArray)
}

fun <T> findSchema(clazz: Class<T>): Schema<*>? {
    return getEnumSchema(clazz) ?: when (clazz) {
        String::class.java -> StringSchema()
        Boolean::class.java -> BooleanSchema()
        java.lang.Boolean::class.java -> BooleanSchema()
        Int::class.java -> IntegerSchema()
        Integer::class.java -> IntegerSchema()
        List::class.java -> ArraySchema()
        Long::class.java -> IntegerSchema().format("int64")
        BigDecimal::class.java -> IntegerSchema().format("")
        Date::class.java -> DateSchema()
        LocalDate::class.java -> DateSchema()
        LocalDateTime::class.java -> DateTimeSchema()
        /* BEGIN Custom classes */
        ByteArray::class.java -> StringSchema().format("binary")
        /* END Custom classes */
        else -> ModelConverters.getInstance().read(clazz)[clazz.simpleName]
    }
}

fun <T> getEnumSchema(clazz: Class<T>): Schema<*>? {
    val values = clazz.enumConstants ?: return null

    val schema = StringSchema()
    for (enumVal in values) {
        schema.addEnumItem(enumVal.toString())
    }
    return schema
}


fun <T> MediaType.example(clazz: Class<T>, value: T, init: Example.() -> Unit) {
    if (examples == null) {
        examples = mutableMapOf()
    }

    val example = Example()
    example.value = value
    example.init()
    examples[clazz.simpleName] = example
}

