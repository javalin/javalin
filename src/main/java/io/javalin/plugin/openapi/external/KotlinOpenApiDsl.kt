package io.javalin.plugin.openapi.external

import io.javalin.plugin.openapi.annotations.SchemaType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

// The original functions are in https://github.com/derveloper/kotlin-openapi3-dsl/blob/master/src/main/kotlin/cc/vileda/openapi/dsl/OpenApiDsl.kt
// I needed to create a copy of all the inline functions, as it is not possible to call kotlin inline function from java code

internal fun Components.schema(clazz: Array<Class<*>>) {
    schema(clazz) {}
}

internal fun Components.schema(clazz: Array<Class<*>>, init: Schema<*>.() -> Unit) {
    if (schemas == null) {
        schemas = mutableMapOf()
    }
    val schema = when {
        clazz.size == 1 -> findSchema(clazz.first())
        else ->  clazz.composedFindSchemaResponse()
    }
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
    mediaType.schema = refSchema(clazz)
    return mediaType
}

internal fun <T> refSchema(clazz: Class<T>): Schema<T> {
    val schema = Schema<T>()
    schema.`$ref` = clazz.simpleName
    return schema
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

internal fun composedSchema(clazzArray: Array<Class<*>>, type: SchemaType): Schema<*> {
    val composedSchema = ComposedSchema()
    val objSchema = schemaArray(clazzArray)
    return when {
        type == SchemaType.oneOf -> composedSchema.oneOf(objSchema)
        type == SchemaType.allOf -> composedSchema.allOf(objSchema)
        type == SchemaType.anyOf -> composedSchema.anyOf(objSchema)
        else -> {
            LoggerFactory.getLogger(Content::class.java).warn("Attempted to serialize a composed schema but the SchemaType given could not be processed, SchemaType: ${type.name}, added the first object as a return object in stead")
            objSchema.first()
        }
    }
}

fun schemaArray(clazzArray: Array<Class<*>>): List<Schema<*>> {
    return clazzArray.map { refSchema(it) }
}

fun Content.mediaTypeComposed(clazzArray: Array<Class<*>>, type: SchemaType, name: String) {
    val mediaType = MediaType()
    mediaType.schema = composedSchema(clazzArray, type)
    addMediaType(name, mediaType)
}


internal fun Content.mediaTypeComposedArray(clazzArray: Array<Class<*>>, type: SchemaType, name: String) {
    val mediaType = MediaType()
    mediaType.schema = composedSchema(clazzArray, type)
    mediaType.schema.type = "array"
    addMediaType(name, mediaType)
}

data class FindSchemaResponse(
        /** The main schema that is requested */
        val main: Schema<*>,
        /** All schemas that are used by the main schema */
        val all: Map<String, Schema<*>> = mapOf()
)

fun Array<Class<*>>.composedFindSchemaResponse(): FindSchemaResponse {
    val allBuilder = mutableMapOf<String, Schema<*>>()
    val schemaArrayFromClassArray = this.map { findSchemaFromClass(it)?.main }
    schemaArrayFromClassArray.forEach { schema -> schema?.name?.let { allBuilder.put(key = it, value =  schema) } }
    return FindSchemaResponse(main = Schema<Any>() ,all = allBuilder);
}

internal fun <T> findSchema(clazz: Class<T>): FindSchemaResponse? {
    getEnumSchema(clazz)?.let {
        return FindSchemaResponse(it)
    }
    return findSchemaFromClass(clazz)
}

enum class FindschemaResponseMap(val javaClass: Class<*>?, val schema: Schema<*>) {
    STRING(String::class.java, StringSchema()),
    BOOL(Boolean::class.java, BooleanSchema()),
    BOOL_JAVA(java.lang.Boolean::class.java, BooleanSchema()),
    INT(Int::class.java, IntegerSchema()),
    INTEGER(Integer::class.java, IntegerSchema()),
    LIST(List::class.java, ArraySchema()),
    MAP(Map::class.java, MapSchema()),
    LONG(Long::class.java, IntegerSchema().format("int64")),
    BIGDECIMAL(BigDecimal::class.java, IntegerSchema().format("")),
    DATE(Date::class.java, DateSchema()),
    LOCALDATE(LocalDate::class.java, DateSchema()),
    LOCALDATETIME(LocalDateTime::class.java, DateTimeSchema()),
    BYTEARRAY(ByteArray::class.java, StringSchema().format("binary")),
    //COMPOSED(),
    OTHER(null, ObjectSchema());

    fun getFindSchema(): FindSchemaResponse {
        return FindSchemaResponse(main = schema);
    }

}
fun findSchemaFromClass(javaClass: Class<*>): FindSchemaResponse? {
    return FindschemaResponseMap.values().find { it.javaClass == javaClass }?.getFindSchema() ?: specialClassSchema(javaClass);
}

fun specialClassSchema(clazz: Class<*>): FindSchemaResponse? {
    val schemas = ModelConverters.getInstance().readAll(clazz)
    return schemas[clazz.simpleName]?.let { FindSchemaResponse(it, schemas) }
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

