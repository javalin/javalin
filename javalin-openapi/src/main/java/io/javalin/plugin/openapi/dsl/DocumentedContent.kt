package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ComposedType
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.mediaType
import io.javalin.plugin.openapi.external.mediaTypeArrayOf
import io.javalin.plugin.openapi.external.mediaTypeArrayOfRef
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.javalin.plugin.openapi.external.schema
import io.javalin.plugin.openapi.openApiExamples
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
@JvmSynthetic
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

    /** This constructor overrides the schema directly. The `from` class won't be used anymore */
    constructor(schema: Schema<*>, contentType: String) : this(
            Object::class.java,
            schema.type == "array",
            contentType,
            schema
    )

    fun isNonRefType(): Boolean = if (schema == null) {
        fromType.isNonRefType()
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

fun Class<*>.isNonRefType(): Boolean = this in nonRefTypes

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
        Float::class.java,
        java.lang.Float::class.java,
        List::class.java,
        Long::class.java,
        java.lang.Long::class.java,
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
    openApiExamples[documentedContent.fromType]?.let { examples ->
        get(documentedContent.contentType)?.examples = examples
    }
}

fun Components.applyDocumentedContent(documentedResponse: DocumentedContent) {
    if (!documentedResponse.isNonRefType()) {
        schema(documentedResponse.fromType)
    }
}
