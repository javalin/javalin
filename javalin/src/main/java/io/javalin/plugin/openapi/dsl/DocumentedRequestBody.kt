package io.javalin.plugin.openapi.dsl

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.parameters.RequestBody

class DocumentedRequestBody(
        val content: List<DocumentedContent>
)

fun RequestBody.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    if (documentedRequestBody.content.isNotEmpty()) {
        updateContent {
            documentedRequestBody.content.forEach { applyDocumentedContent(it) }
        }
    }
}

fun Components.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    documentedRequestBody.content.forEach { applyDocumentedContent(it) }
}
