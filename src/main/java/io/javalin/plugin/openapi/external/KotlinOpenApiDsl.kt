package io.javalin.plugin.openapi.external

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// The original functions are in https://github.com/derveloper/kotlin-openapi3-dsl/blob/master/src/main/kotlin/cc/vileda/openapi/dsl/OpenApiDsl.kt
// I needed to create a copy of all the inline functions, as it is not possible to call kotlin inline function from java code

internal fun <T> Components.schema(clazz: Class<T>) {
    schema(clazz) {}
}

internal fun <T> Components.schema(clazz: Class<T>, init: Schema<*>.() -> Unit) {
    if (schemas == null) {
        schemas = mutableMapOf()
    }
    val schema = findSchema(clazz)
    schema?.main?.init()
    schema?.all?.forEach { (key, schema) ->
        schemas[key] = schema
    }
}

internal fun <T> Parameter.schema(clazz: Class<T>, init: Schema<*>.() -> Unit) {
    schema = findSchema(clazz)?.main
    schema.init()
}

internal fun <T> Parameter.schema(clazz: Class<T>) {
    schema = findSchema(clazz)?.main
}

internal fun <T> mediaType(clazz: Class<T>): MediaType {
    val mediaType = MediaType()
    val modelSchema = findSchema(clazz)?.main
    mediaType.schema = modelSchema
    return mediaType
}

/**
 * This helper is not in the original library.
 * Because it is not possible to create a list instance of an unknown type we need a separate method for that.
 */
internal fun mediaTypeOfList(): MediaType {
    val mediaType = MediaType()
    val modelSchema = ArraySchema()
    mediaType.schema = modelSchema
    return mediaType
}

internal fun <T> mediaTypeRef(clazz: Class<T>): MediaType {
    val mediaType = MediaType()
    mediaType.schema = Schema<T>()
    mediaType.schema.`$ref` = clazz.simpleName
    return mediaType
}

internal fun <T> Content.mediaTypeRef(clazz: Class<T>, name: String) {
    val mediaType = mediaTypeRef(clazz)
    addMediaType(name, mediaType)
}

internal fun <T> Content.mediaType(clazz: Class<T>, name: String) {
    val mediaType = mediaType(clazz)
    addMediaType(name, mediaType)
}

internal fun <T> Content.mediaTypeArrayOfRef(clazz: Class<T>, name: String) {
    val mediaTypeArray = mediaTypeOfList()
    val mediaTypeObj = mediaTypeRef(clazz)
    (mediaTypeArray.schema as ArraySchema).items(mediaTypeObj.schema)
    addMediaType(name, mediaTypeArray)
}

internal fun <T> Content.mediaTypeArrayOf(clazz: Class<T>, name: String) {
    val mediaTypeArray = mediaTypeOfList()
    val mediaTypeObj = mediaType(clazz)
    (mediaTypeArray.schema as ArraySchema).items(mediaTypeObj.schema)
    addMediaType(name, mediaTypeArray)
}

internal data class FindSchemaResponse(
        /** The main schema that is requested */
        val main: Schema<*>,
        /** All schemas that are used by the main schema */
        val all: Map<String, Schema<*>> = mapOf()
)

internal fun <T> findSchema(clazz: Class<T>): FindSchemaResponse? {
    getEnumSchema(clazz)?.let {
        return FindSchemaResponse(it)
    }
    return when (clazz) {
        String::class.java -> FindSchemaResponse(StringSchema())
        Boolean::class.java -> FindSchemaResponse(BooleanSchema())
        java.lang.Boolean::class.java -> FindSchemaResponse(BooleanSchema())
        Int::class.java -> FindSchemaResponse(IntegerSchema())
        Integer::class.java -> FindSchemaResponse(IntegerSchema())
        List::class.java -> FindSchemaResponse(ArraySchema())
        Map::class.java -> FindSchemaResponse(MapSchema())
        Long::class.java -> FindSchemaResponse(IntegerSchema().format("int64"))
        java.lang.Long::class.java -> FindSchemaResponse(IntegerSchema().format("int64"))
        BigDecimal::class.java -> FindSchemaResponse(IntegerSchema().format(""))
        Date::class.java -> FindSchemaResponse(DateSchema())
        LocalDate::class.java -> FindSchemaResponse(DateSchema())
        LocalDateTime::class.java -> FindSchemaResponse(DateTimeSchema())
        Double::class.java -> FindSchemaResponse(NumberSchema())
        java.lang.Double::class.java -> FindSchemaResponse(NumberSchema())
        Float::class.java -> FindSchemaResponse(NumberSchema())
        java.lang.Float::class.java -> FindSchemaResponse(NumberSchema())
        /* BEGIN Custom classes */
        ByteArray::class.java -> FindSchemaResponse(StringSchema().format("binary"))
        /* END Custom classes */
        else -> {
            val resolved = ModelConverters.getInstance().readAllAsResolvedSchema(clazz)
            return FindSchemaResponse(resolved.schema, resolved.referencedSchemas + mapOf(clazz.simpleName to resolved.schema))
        }
    }
}

internal fun <T> getEnumSchema(clazz: Class<T>): Schema<*>? {
    val values = clazz.enumConstants ?: return null

    val schema = StringSchema()
    for (enumVal in values) {
        schema.addEnumItem(enumVal.toString())
    }
    return schema
}


internal fun <T> MediaType.example(clazz: Class<T>, value: T, init: Example.() -> Unit) {
    if (examples == null) {
        examples = mutableMapOf()
    }

    val example = Example()
    example.value = value
    example.init()
    examples[clazz.simpleName] = example
}

