package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.utils.updateContent
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.responses.ApiResponse

class DocumentedResponse(
        val status: String,
        val content: DocumentedContent?
)


fun ApiResponse.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    description = ""
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
