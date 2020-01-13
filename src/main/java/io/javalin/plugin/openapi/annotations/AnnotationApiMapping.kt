package io.javalin.plugin.openapi.annotations

import io.javalin.plugin.openapi.dsl.DocumentedContent
import io.javalin.plugin.openapi.dsl.DocumentedParameter
import io.javalin.plugin.openapi.dsl.DocumentedResponse
import io.javalin.plugin.openapi.dsl.OpenApiDocumentation
import io.javalin.plugin.openapi.dsl.createUpdater
import io.javalin.plugin.openapi.dsl.anyOf
import io.javalin.plugin.openapi.dsl.oneOf
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.security.SecurityScheme
import kotlin.reflect.KClass

fun OpenApi.asOpenApiDocumentation(): OpenApiDocumentation {
    val documentation = OpenApiDocumentation()
    val annotation = this

    documentation.isIgnored = annotation.ignore

    documentation.operation { it.applyAnnotation(annotation) }

    annotation.cookies.forEach { documentation.applyParamAnnotation("cookie", it) }
    annotation.headers.forEach { documentation.applyParamAnnotation("header", it) }
    annotation.pathParams.forEach { documentation.applyParamAnnotation("path", it) }
    annotation.queryParams.forEach { documentation.applyParamAnnotation("query", it) }

    annotation.formParams.forEach { documentation.formParam(it.name, it.type.java, it.required) }

    documentation.applyRequestBodyAnnotation(annotation.requestBody)
    documentation.applyComposedRequestBodyAnnotation(annotation.composedRequestBody)

    annotation.fileUploads.forEach { fileUpload ->
        if (fileUpload.isArray) {
            documentation.uploadedFiles(name = fileUpload.name) { it.applyAnnotation(fileUpload) }
        } else {
            documentation.uploadedFile(name = fileUpload.name) { it.applyAnnotation(fileUpload) }
        }
    }
    documentation.applyResponseAnnotations(annotation.responses)

    documentation.applySecurityAnnotations(annotation.security)

    return documentation
}

private fun resolveNullValueFromContentType(fromType: KClass<*>, contentType: String): KClass<out Any> {
    return if (fromType == NULL_CLASS::class) {
        if (contentType.startsWith("text/")) {
            // Default for text/html, etc is string
            String::class
        } else {
            // Default for everything else is unit
            Unit::class
        }
    } else {
        fromType
    }
}

private fun Operation.applyAnnotation(annotation: OpenApi) {
    if (annotation.description.isNotNullString()) {
        this.description = annotation.description
    }
    if (annotation.summary.isNotNullString()) {
        this.summary = annotation.summary
    }
    if (annotation.operationId.isNotNullString()) {
        this.operationId = annotation.operationId
    }
    if (annotation.deprecated) {
        this.deprecated = annotation.deprecated
    }
    annotation.tags.forEach { tag -> this.addTagsItem(tag) }
}

private fun RequestBody.applyAnnotation(annotation: OpenApiRequestBody) {
    if (annotation.required) {
        this.required = annotation.required
    }
    if (annotation.description.isNotNullString()) {
        this.description = annotation.description
    }
}

private fun RequestBody.applyAnnotation(annotation: OpenApiComposedRequestBody) {
    if (annotation.required) {
        this.required = annotation.required
    }
    if (annotation.description.isNotNullString()) {
        this.description = annotation.description
    }
}

private fun RequestBody.applyAnnotation(annotation: OpenApiFileUpload) {
    if (annotation.required) {
        this.required = annotation.required
    }
    if (annotation.description.isNotNullString()) {
        this.description = annotation.description
    }
}

private fun OpenApiContent.asDocumentedContent(): DocumentedContent {
    val content = this
    val from = resolveNullValueFromContentType(content.from, content.type)
    return DocumentedContent(
            from = from.java,
            isArray = content.isArray,
            contentType = content.type
    )
}

private fun OpenApiDocumentation.applyRequestBodyAnnotation(requestBody: OpenApiRequestBody) {
    if (requestBody.content.isNotEmpty()) {
        this.body(requestBody.content.map { it.asDocumentedContent() }, createUpdater { it.applyAnnotation(requestBody) })
    }
}

private fun OpenApiDocumentation.applyComposedRequestBodyAnnotation(requestBody: OpenApiComposedRequestBody) {
    val composition = when {
        requestBody.anyOf.isNotEmpty() -> anyOf(*requestBody.anyOf.toList().map { it.asDocumentedContent() }.toTypedArray())
        requestBody.oneOf.isNotEmpty() -> oneOf(*requestBody.oneOf.toList().map { it.asDocumentedContent() }.toTypedArray())
        else -> null
    }
    if (composition != null) {
        this.body(composition, requestBody.contentType, createUpdater { it.applyAnnotation(requestBody) })
    }
}

private fun OpenApiDocumentation.applyResponseAnnotations(responses: Array<OpenApiResponse>) {
    val documentation = this
    responses.groupBy { it.status }.forEach { (status, list) ->
        documentation.result(
                documentedResponse = DocumentedResponse(
                        status = status,
                        content = list.flatMap { it.content.toList() }.map { it.asDocumentedContent() }
                ),
                applyUpdates = { responseDocumentation ->
                    val description = list.map { it.description }.filter { it.isNotNullString() }.joinToString(separator = "; ")
                    if (description.isNotBlank()) {
                        responseDocumentation.description = description
                    }
                }
        )
    }
}

private fun OpenApiDocumentation.applyParamAnnotation(`in`: String, param: OpenApiParam) {
    val documentation = this
    documentation.param(
            documentedParameter = createDocumentedParam(`in`, param),
            applyUpdates = { paramDocumentation ->
                if (param.description.isNotNullString()) {
                    paramDocumentation.description = param.description
                }
                if (param.required) {
                    paramDocumentation.required = param.required
                }
                if (param.deprecated) {
                    paramDocumentation.deprecated = param.deprecated
                }
                if (param.allowEmptyValue) {
                    paramDocumentation.allowEmptyValue = param.allowEmptyValue
                }
            }
    )
}

private fun OpenApiDocumentation.applySecurityAnnotations(security: Array<OpenApiSecurity>) {
    val documentation = this
    security.forEach {
        when(it.scheme) {
            AuthScheme.BASIC -> documentation.basicAuth()
            AuthScheme.BEARER -> documentation.bearerAuth(format = if (it.format != NULL_STRING) it.format else null)
            else -> {
                if (it.name != NULL_STRING) {
                    when(it.scheme) {
                        AuthScheme.APIKEY_COOKIE -> documentation.apiKeyAuth(it.name, `in` = SecurityScheme.In.COOKIE)
                        AuthScheme.APIKEY_HEADER -> documentation.apiKeyAuth(it.name, `in` = SecurityScheme.In.HEADER)
                        else -> documentation.apiKeyAuth(it.name, `in` = SecurityScheme.In.QUERY)
                    }
                }
            }
        }
    }
}

private fun createDocumentedParam(`in`: String, param: OpenApiParam) = DocumentedParameter(
        `in` = `in`,
        name = param.name,
        type = param.type.java
)

private fun String.isNullString() = this == NULL_STRING
private fun String.isNotNullString() = !this.isNullString()
private fun String.correctNullValue(): String? = if (this.isNullString()) null else this
