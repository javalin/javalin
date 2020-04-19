package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ComposedType
import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.findSchema
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.parameters.RequestBody

class DocumentedRequestBody(
        val content: List<DocumentedContent>,
        val contentType: String? = null,
        val composedType: ComposedType = ComposedType.NULL
)

fun RequestBody.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    if (documentedRequestBody.content.isNotEmpty()) {
        if (documentedRequestBody.composedType != ComposedType.NULL) {
            val composedList = documentedRequestBody.content.map {
                val schema = if (!it.isNonRefType()) mediaTypeRef(it.fromType).schema
                else findSchema(it.fromType)?.main
                if (it.isArray) ArraySchema().items(schema) else schema
            }

            val schema = ComposedSchema().apply {
                when (documentedRequestBody.composedType) {
                    ComposedType.ANY_OF -> anyOf(composedList)
                    ComposedType.ONE_OF -> oneOf(composedList)
                }
            }
            val content = DocumentedContent(schema, documentedRequestBody.contentType ?: ContentType.AUTODETECT)

            updateContent {
                applyDocumentedContent(content)
            }
        } else {
            updateContent {
                documentedRequestBody.content.forEach { applyDocumentedContent(it) }
            }
        }
    }
}

fun Components.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    documentedRequestBody.content.forEach { applyDocumentedContent(it) }
}
