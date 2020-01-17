package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ComposedType
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.OpenApiExample
import io.javalin.plugin.openapi.external.mediaType
import io.javalin.plugin.openapi.external.mediaTypeArrayOf
import io.javalin.plugin.openapi.external.mediaTypeArrayOfRef
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.javalin.plugin.openapi.external.schema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import java.lang.reflect.Modifier
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.javaMethod

@JvmOverloads
fun documentedContent(
        clazz: Class<*>,
        isArray: Boolean = false,
        contentType: String? = ContentType.AUTODETECT
): DocumentedContent {
    return DocumentedContent(
            clazz,
            contentType = contentType,
            isArray = isArray
    )
}

/** Kotlin factory for documented content */
inline fun <reified T> documentedContent(
        contentType: String? = ContentType.AUTODETECT,
        isArray: Boolean = false
): DocumentedContent {
    return DocumentedContent(
            T::class.java,
            contentType = contentType,
            isArray = isArray
    )
}

class DocumentedContent @JvmOverloads constructor(
        from: Class<*>,
        isArray: Boolean = false,
        contentType: String? = null,
        val schema: Schema<*>? = null
) {
    val contentType: String = if (contentType == null || contentType == ContentType.AUTODETECT) {
        from.guessContentType()
    } else {
        contentType
    }

    private val fromTypeIsArray = from.isArray

    private val isNotByteArray = if (schema == null) {
        from != ByteArray::class.java
    } else {
        schema.type == "string" && schema.format == "application/octet-stream"
    }

    val fromType = when {
        fromTypeIsArray && isNotByteArray -> from.componentType!!
        else -> from
    }

    val isArray = if (schema == null) {
        fromTypeIsArray && isNotByteArray || isArray
    } else {
        schema.type == "array"
    }

    val examples = if (!isNonRefType()) {
        from.declaredMethods
                .filter { Modifier.isStatic(it.modifiers) }
                .toMutableList()
                .apply {
                    from.kotlin.companionObject?.declaredFunctions?.map { it.javaMethod }?.forEach { add(it) } // kotlin companion functions
                }
                .map { it to it.getAnnotation(OpenApiExample::class.java) } // map function to annotation
                .filter { it.second != null && it.first.parameterCount == 0 }
                .map {
                    it.first.isAccessible = true
                    val example = Example().apply {
                        value = it.first.invoke(from.kotlin.companionObjectInstance) // in case of kotlin companion
                    }
                    it.second.name to example
                }.toMap()
    } else {
        emptyMap()
    }

    /** This constructor overrides the schema directly. The `from` class won't be used anymore */
    constructor(schema: Schema<*>, contentType: String) : this(
            Object::class.java,
            schema.type == "array",
            contentType,
            schema
    )

    fun isNonRefType(): Boolean = if (schema == null) {
        fromType in nonRefTypes
    } else {
        true
    }
}

sealed class Composition(val type: ComposedType, val content: List<DocumentedContent>) {

    class OneOf(contents: List<DocumentedContent>) : Composition(ComposedType.ONE_OF, contents)
    class AnyOf(contents: List<DocumentedContent>) : Composition(ComposedType.ANY_OF, contents)

}

fun oneOf(vararg content: DocumentedContent) = Composition.OneOf(content.toList())
fun anyOf(vararg content: DocumentedContent) = Composition.AnyOf(content.toList())

/**
 * Try to determine the content type based on the class
 */
fun Class<*>.guessContentType(): String =
        when (this) {
            String::class.java -> "text/plain"
            ByteArray::class.java -> "application/octet-stream"
            else -> "application/json"
        }

/**
 * A set of types that should be inlined, instead of creating a reference, in the OpenApi documentation
 */
val nonRefTypes = setOf(
        String::class.java,
        Boolean::class.java,
        java.lang.Boolean::class.java,
        Int::class.java,
        Integer::class.java,
        Double::class.java,
        java.lang.Double::class.java,
        List::class.java,
        Long::class.java,
        BigDecimal::class.java,
        Date::class.java,
        LocalDate::class.java,
        LocalDateTime::class.java,
        ByteArray::class.java,
        Instant::class.java
)

fun Content.applyDocumentedContent(documentedContent: DocumentedContent) {
    when {
        documentedContent.schema != null -> addMediaType(
                documentedContent.contentType,
                MediaType().apply { schema = documentedContent.schema }
        )
        documentedContent.isNonRefType() -> when {
            documentedContent.isArray -> mediaTypeArrayOf(documentedContent.fromType, documentedContent.contentType)
            else -> mediaType(documentedContent.fromType, documentedContent.contentType)
        }
        else -> when {
            documentedContent.isArray -> mediaTypeArrayOfRef(documentedContent.fromType, documentedContent.contentType)
            else -> mediaTypeRef(documentedContent.fromType, documentedContent.contentType)
        }
    }

    get(documentedContent.contentType)?.examples = documentedContent.examples
}

fun Components.applyDocumentedContent(documentedResponse: DocumentedContent) {
    if (!documentedResponse.isNonRefType()) {
        schema(documentedResponse.fromType)
    }
}
