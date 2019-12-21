package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.external.findSchema
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.eclipse.jetty.http.HttpStatus

class DocumentedResponse(
        val status: String,
        val content: List<DocumentedContent>
)

fun DocumentedResponse.getStatusMessage() = status.toIntOrNull()?.let { HttpStatus.getMessage(it) } ?: ""

fun ApiResponse.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    description = description ?: documentedResponse.getStatusMessage()

    val contentToApply = arrayListOf<DocumentedContent>()
    documentedResponse.content.groupBy { it.contentType }.forEach { (contentType, list) ->
        if (list.size > 1) {
            val composedList = list.map {
                val schema = if (!it.isNonRefType()) mediaTypeRef(it.fromType).schema
                else findSchema(it.fromType)?.main
                if (it.isArray) ArraySchema().items(schema) else schema
            }

            val schema = ComposedSchema().apply {
                oneOf(composedList)
            }
            val content = DocumentedContent(schema, contentType)
            contentToApply.add(content)
        } else {
            contentToApply.addAll(list)
        }
    }

    if (contentToApply.isNotEmpty()) {
        updateContent {
            contentToApply.forEach { applyDocumentedContent(it) }
        }
    }

}

fun Components.applyDocumentedResponse(documentedResponse: DocumentedResponse) {
    documentedResponse.content.forEach { applyDocumentedContent(it) }
}
