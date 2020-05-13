package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.external.findSchema
import io.javalin.plugin.openapi.external.mediaTypeRef
import io.javalin.plugin.openapi.external.schema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.parameters.Parameter

class DocumentedParameter(
        val `in`: String,
        val name: String,
        val type: Class<*>
)

fun Parameter.applyDocumentedParameter(documentedParameter: DocumentedParameter) {
    `in` = documentedParameter.`in`
    name = documentedParameter.name
    schema = if (documentedParameter.type.isNonRefType()) {
        findSchema(documentedParameter.type)?.main
    } else {
        mediaTypeRef(documentedParameter.type).schema
    }
}

fun Parameter.applyRepeatableDocumentedParameter(documentedParameter: DocumentedParameter) {
    `in` = documentedParameter.`in`
    name = documentedParameter.name
    schema = if (documentedParameter.type.isNonRefType()) {
        findSchema(documentedParameter.type)?.main?.let { ArraySchema().items(it) }
    } else {
        ArraySchema().items(mediaTypeRef(documentedParameter.type).schema)
    }
}

fun Components.applyDocumentedParameter(documentedParameter: DocumentedParameter) {
    if (!documentedParameter.type.isNonRefType()) {
        this.schema(documentedParameter.type)
    }
}
