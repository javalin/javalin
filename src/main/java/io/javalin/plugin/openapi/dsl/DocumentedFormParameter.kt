package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.findSchema
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.RequestBody

class DocumentedFormParameter(
        val name: String,
        val clazz: Class<*>,
        val required: Boolean = false
)

class DocumentedFileUpload(
        val name: String,
        val isMultipleFiles: Boolean = false
)

fun getFileSchema(fileUpload: DocumentedFileUpload): Schema<*>? {
    return if (fileUpload.isMultipleFiles) {
        ArraySchema().items(findSchema(ByteArray::class.java)?.main)
    } else {
        findSchema(ByteArray::class.java)?.main
    }
}

fun RequestBody.applyDocumentedFormParameters(documentedFormParameters: List<DocumentedFormParameter>,
                                              fileUploadList: List<DocumentedFileUpload>) {
    if (documentedFormParameters.isNotEmpty() || fileUploadList.isNotEmpty()) {

        val formParams = documentedFormParameters
                .map { it.name to findSchema(it.clazz)?.main }
        val fileParams = fileUploadList
                .map { it.name to getFileSchema(it) }

        val schema = ObjectSchema().apply {
            properties = fileParams.union(formParams).toMap()
            required = documentedFormParameters
                    .filter { it.required }
                    .map { it.name }
        }

        // Requests with file uploads need to be a multipart content
        // Regular forms alone will be url encoded by default
        val contentType = if (fileUploadList.isEmpty()) ContentType.FORM_DATA_URL_ENCODED else ContentType.FORM_DATA_MULTIPART
        val content = DocumentedContent(schema, contentType)

        updateContent { applyDocumentedContent(content) }
    }
}
