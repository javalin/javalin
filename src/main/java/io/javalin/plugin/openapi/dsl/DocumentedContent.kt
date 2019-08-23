package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.SchemaType
import io.javalin.plugin.openapi.external.*
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import org.slf4j.LoggerFactory
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
        from: Array<Class<*>>,
        isArray: Boolean = false,
        contentType: String? = null,
        val schemaType: SchemaType = SchemaType.NULL,
        val schema: Schema<*>? = null
) {

    var log = LoggerFactory.getLogger(DocumentedContent::class.java)


    val contentType: String = if (contentType == null || contentType == ContentType.AUTODETECT) {
        log.info("Guessing content type from the first content") //FIXME: is this allowed, shoudl I be less chatty?
        from.first().guessContentType()
    } else {
        contentType
    }

    private val fromTypeIsArray = from.all { it.isArray }

    private val isNotByteArray = if (schema == null) {
        from != ByteArray::class.java
    } else {
        schema.type == "string" && schema.format == "application/octet-stream"
    }

    val fromType = when {
        fromTypeIsArray && isNotByteArray -> from.first().componentType!!
        else -> from.first()
    }

    val fromArray: Array<Class<*>> = from //FIXME: because it is explicitly an array; old understanding is kept

    val isArray = if (schema == null) {
        fromTypeIsArray && isNotByteArray || isArray
    } else {
        schema.type == "array"
    }

    /** This constructor overrides the schema directly. The `from` class won't be used anymore */
    constructor(schema: Schema<*>, contentType: String) : this(
            arrayOf(Object::class.java),
            schema.type == "array",
            contentType,
            SchemaType.NULL,
            schema
    )

    /** Backwards compatible constructor */
    constructor(from: Class<*>, isArray: Boolean, contentType: String? = null) : this(
            arrayOf(from),
            isArray,
            contentType,
            SchemaType.NULL,
            null
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
            documentedContent.isArray -> when {
                documentedContent.schemaType != SchemaType.NULL -> {
                    mediaTypeComposedArray(documentedContent.fromArray, documentedContent.schemaType, documentedContent.contentType)
                }
                else -> {
                    mediaTypeArrayOfRef(documentedContent.fromType, documentedContent.contentType)
                }
            }
            else ->  when {
                documentedContent.schemaType != SchemaType.NULL -> {
                    mediaTypeComposed(documentedContent.fromArray, documentedContent.schemaType, documentedContent.contentType)
                }
                else -> {
                    mediaTypeRef(documentedContent.fromType, documentedContent.contentType)
                }
        }
    }
    }
}

fun Components.applyDocumentedContent(documentedResponse: DocumentedContent) {
    if (!documentedResponse.isNonRefType()) {
        schema(documentedResponse.fromArray)
    }
}
