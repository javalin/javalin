package io.javalin.plugin.openapi.dsl

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

class DocumentedContent(
        returnType: Class<*>,
        isArray: Boolean,
        contentType: String? = null,
        val schema: Schema<*>? = null
) {
    val contentType: String = contentType ?: returnType.guessContentType()

    private val returnTypeIsArray = returnType.isArray

    private val isNotByteArray = if (schema == null) {
        returnType != ByteArray::class.java
    } else {
        schema.type == "string" && schema.format == "application/octet-stream"
    }

    val returnType = when {
        returnTypeIsArray && isNotByteArray -> returnType.componentType!!
        else -> returnType
    }

    val isArray = if (schema == null) {
        returnTypeIsArray && isNotByteArray || isArray
    } else {
        schema.type == "array"
    }

    /** This constructor overrides the schema directly. The returnType won't be used anymore */
    constructor(schema: Schema<*>, contentType: String) : this(
            Object::class.java,
            schema.type == "array",
            contentType,
            schema
    )

    fun isNonRefType(): Boolean = if (schema == null) {
        returnType in nonRefTypes
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
            documentedContent.isArray -> mediaTypeArrayOf(documentedContent.returnType, documentedContent.contentType)
            else -> mediaType(documentedContent.returnType, documentedContent.contentType)
        }
        else -> when {
            documentedContent.isArray -> mediaTypeArrayOfRef(documentedContent.returnType, documentedContent.contentType)
            else -> mediaTypeRef(documentedContent.returnType, documentedContent.contentType)
        }
    }
}

fun Components.applyDocumentedContent(documentedResponse: DocumentedContent) {
    if (!documentedResponse.isNonRefType()) {
        schema(documentedResponse.returnType)
    }
}
