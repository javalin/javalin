package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.external.findSchema
import io.swagger.v3.oas.models.parameters.Parameter

class DocumentedParameter(
        val `in`: String,
        val name: String,
        val type: Class<*>
)

fun Parameter.applyDocumentedParameter(documentedParameter: DocumentedParameter) {
    `in` = documentedParameter.`in`
    name = documentedParameter.name
    schema = findSchema(documentedParameter.type)?.main
}
