package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.mediaType
import io.javalin.plugin.openapi.external.mediaTypeArrayOf
import io.javalin.plugin.openapi.external.mediaTypeArrayOfRef
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.javalin.plugin.openapi.external.schema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

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
        Int::class.java,
        Integer::class.java,
        List::class.java,
        Long::class.java,
        BigDecimal::class.java,
        Date::class.java,
        LocalDate::class.java,
        LocalDateTime::class.java,
        ByteArray::class.java
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
}

fun Components.applyDocumentedContent(documentedResponse: DocumentedContent) {
    if (!documentedResponse.isNonRefType()) {
        schema(documentedResponse.fromType)
    }
}
