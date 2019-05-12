package io.javalin.plugin.openapi.dsl

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.responses.ApiResponse
import org.eclipse.jetty.http.HttpStatus

class DocumentedResponse(
        val status: String,
        val content: DocumentedContent?
)

fun DocumentedResponse.getStatusMessage() = status.toIntOrNull()?.let { HttpStatus.getMessage(it) } ?: ""

fun ApiResponse.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    description = documentedResponse.getStatusMessage()
    if (documentedResponse.content != null) {
        updateContent {
            applyDocumentedContent(documentedResponse.content)
        }
    }
}

fun Components.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    if (documentedResponse.content != null) {
        applyDocumentedContent(documentedResponse.content)
    }
}
