package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.findSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.parameters.RequestBody

class DocumentedFormParameter(
        val name: String,
        val clazz: Class<*>
)

fun RequestBody.applyDocumentedFormParameters(documentedFormParameters: List<DocumentedFormParameter>) {
    if (documentedFormParameters.isNotEmpty()) {
        val schema = ObjectSchema().apply {
            properties = documentedFormParameters
                    .map { it.name to findSchema(it.clazz)?.main }
                    .toMap()
        }
        val content = DocumentedContent(schema, ContentType.FORM_DATA)

        updateContent { applyDocumentedContent(content) }
    }
}
