package io.javalin.plugin.openapi.dsl

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
                                              fileUploadList: List<DocumentedFileUpload>,
                                              contentType: String) {
    if (documentedFormParameters.isNotEmpty() || fileUploadList.isNotEmpty()) {
        val schema = ObjectSchema().apply {
            val formParams = documentedFormParameters
                .map { it.name to findSchema(it.clazz)?.main }
            val fileParams = fileUploadList
                .map { it.name to getFileSchema(it) }
            properties = fileParams.union(formParams).toMap()
        }
        schema.required = documentedFormParameters
            .filter { it.required }
            .map { it.name }
        val content = DocumentedContent(schema, contentType)

        updateContent { applyDocumentedContent(content) }
    }
}
