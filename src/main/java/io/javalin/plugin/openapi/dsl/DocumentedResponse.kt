package io.javalin.plugin.openapi.dsl

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.responses.ApiResponse
import org.eclipse.jetty.http.HttpStatus

class DocumentedResponse(
        val status: String,
        val content: List<DocumentedContent>
)

fun DocumentedResponse.getStatusMessage() = status.toIntOrNull()?.let { HttpStatus.getMessage(it) } ?: ""

fun ApiResponse.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    description = description ?: documentedResponse.getStatusMessage()
    if (documentedResponse.content.isNotEmpty()) {
        updateContent {
            documentedResponse.content.forEach { applyDocumentedContent(it) }
        }
    }
}

fun Components.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    documentedResponse.content.forEach { applyDocumentedContent(it) }
}
