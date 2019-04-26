package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.utils.updateContent
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.parameters.RequestBody

class DocumentedRequestBody(
        val content: DocumentedContent
)

fun RequestBody.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    updateContent {
        applyDocumentedContent(documentedRequestBody.content)
    }
}

fun Components.applyDocumentedRequestBody(documentedRequestBody: DocumentedRequestBody) {
    applyDocumentedContent(documentedRequestBody.content)
}
