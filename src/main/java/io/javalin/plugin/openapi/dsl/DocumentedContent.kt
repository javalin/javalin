package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.annotations.NULL_CLASS
import io.javalin.plugin.openapi.annotations.OpenApiContent
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

    val from: Array<Class<*>> = if (from.isNullOrEmpty()) arrayOf(NULL_CLASS::class.java) else from

    val contentType: String = if (contentType == null || contentType == ContentType.AUTODETECT) {
        from.first().guessContentType()
    } else {
        contentType
    }

    private val fromTypeIsArray = from.all { it.isArray }

    private val isNotByteArray = if (schema == null) {
        from.first() != ByteArray::class.java
    } else {
        schema.type == "string" && schema.format == "application/octet-stream"
    }

    val fromType = when {
        fromTypeIsArray && isNotByteArray -> from.first().componentType!!
        else -> from.first()
    }

    val fromArray: Array<Class<*>> = from

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
        (ContentTypeClassRelation.values().find { this == it.javaClass } ?: ContentTypeClassRelation.OTHER).contentType.first()

fun OpenApiContent.fillSchemaClassFromContentType(): Class<*> =
        (ContentTypeClassRelation.values().find { it.contentType.contains(this.type) } ?: ContentTypeClassRelation.OTHER).javaClass

enum class ContentTypeClassRelation(val javaClass: Class<*>, val contentType: Array<String>) {
    TEXT(String::class.java, arrayOf(ContentType.PLAIN, ContentType.HTML)),
    OCTET(ByteArray::class.java, arrayOf(ContentType.OCTET)),
    OTHER(Object::class.java, arrayOf(ContentType.JSON));
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
