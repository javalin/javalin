package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ContentType
import io.javalin.plugin.openapi.external.findSchema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse

class OpenApiDocumentation {
    var isIgnored: Boolean? = null
    val operationUpdaterList = mutableListOf<OpenApiUpdater<Operation>>()
    val requestBodyList = mutableListOf<OpenApiUpdater<RequestBody>>()
    val parameterUpdaterListMapping = mutableMapOf<String, MutableList<OpenApiUpdater<Parameter>>>()
    val responseUpdaterListMapping = mutableMapOf<String, MutableList<OpenApiUpdater<ApiResponse>>>()
    val componentsUpdaterList = mutableListOf<OpenApiUpdater<Components>>()

    fun hasRequestBodies(): Boolean = requestBodyList.isNotEmpty()
    fun hasResponses(): Boolean = responseUpdaterListMapping.values.flatten().isNotEmpty()

    /** Hide the endpoint in the documentation */
    @JvmOverloads
    fun ignore(isIgnored: Boolean = true) = apply { this.isIgnored = isIgnored }

    // --- OPERATION ---
    fun operation(applyUpdates: ApplyUpdates<Operation>) = apply {
        operation(createUpdater(applyUpdates))
    }

    fun operation(openApiUpdater: OpenApiUpdater<Operation>) = apply {
        operationUpdaterList.add(openApiUpdater)
    }

    // --- PATH PARAM ---
    inline fun <reified T> pathParam(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        pathParam(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun pathParam(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("path", name, clazz)
        param(documentedQueryParameter, openApiUpdater)
    }

    // --- QUERY PARAM ---
    inline fun <reified T> queryParam(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        queryParam(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun queryParam(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("query", name, clazz)
        param(documentedQueryParameter, openApiUpdater)
    }

    // --- HEADER ---
    inline fun <reified T> header(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        header(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun header(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("header", name, clazz)
        param(documentedQueryParameter, openApiUpdater)
    }

    // --- COOKIE ---
    inline fun <reified T> cookie(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        cookie(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun cookie(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("cookie", name, clazz)
        param(documentedQueryParameter, openApiUpdater)
    }

    // --- PARAM ---
    fun param(documentedParameter: DocumentedParameter, applyUpdates: ApplyUpdates<Parameter>? = null) = apply {
        param(documentedParameter, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun param(documentedParameter: DocumentedParameter, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val parameterUpdaterList = parameterUpdaterListMapping.getOrSetDefault(documentedParameter.name, mutableListOf())
        parameterUpdaterList.add { it.applyDocumentedParameter(documentedParameter) }
        parameterUpdaterList.addIfNotNull(openApiUpdater)
    }

    // --- UPLOADED FILE ---
    fun uploadedFile(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFile(name, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun uploadedFile(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val schema = ObjectSchema().apply {
            properties = mapOf(
                    name to findSchema(ByteArray::class.java)?.main
            )
        }
        body(schema, "multipart/form-data", openApiUpdater)
    }

    // --- UPLOADED FILES ---
    fun uploadedFiles(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFiles(name, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun uploadedFiles(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val schema = ObjectSchema().apply {
            properties = mapOf(
                    name to ArraySchema().items(findSchema(ByteArray::class.java)?.main)
            )
        }
        body(schema, "multipart/form-data", openApiUpdater)
    }

    // --- BODY ---
    inline fun <reified T> body(noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(T::class.java, null, applyUpdates)
    }

    inline fun <reified T> body(contentType: String? = null, noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(T::class.java, contentType, applyUpdates)
    }

    fun body(returnType: Class<*>, contentType: String? = null, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(returnType, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    fun body(returnType: Class<*>, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(returnType, null, openApiUpdater)
    }

    @JvmOverloads
    fun body(returnType: Class<*>, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val documentedContent = listOf(DocumentedContent(returnType, false, contentType))
        body(documentedContent, openApiUpdater)
    }

    @JvmOverloads
    fun body(schema: Schema<*>, contentType: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val documentedContent = listOf(DocumentedContent(schema, contentType))
        body(documentedContent, openApiUpdater)
    }

    @JvmOverloads
    fun body(content: List<DocumentedContent>, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val documentedBody = DocumentedRequestBody(content)
        body(documentedBody, openApiUpdater)
    }

    @JvmOverloads
    fun body(documentedBody: DocumentedRequestBody, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        componentsUpdaterList.add { it.applyDocumentedRequestBody(documentedBody) }
        requestBodyList.add { it.applyDocumentedRequestBody(documentedBody) }
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- BODY AS BYTES ---
    @JvmOverloads
    fun bodyAsBytes(contentType: String? = null, applyUpdates: ApplyUpdates<RequestBody>?) = apply {
        bodyAsBytes(contentType, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun bodyAsBytes(contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(ByteArray::class.java, contentType, openApiUpdater)
    }

    // --- JSON ARRAY ---
    inline fun <reified T> jsonArray(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        jsonArray(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun jsonArray(status: String, returnType: Class<*>, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(returnType, true, "application/json"))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- JSON ---
    inline fun <reified T> json(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        json(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun json(status: String, returnType: Class<*>, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(returnType, false, "application/json"))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- HTML ---
    fun html(status: String, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        html(status, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun html(status: String, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(String::class.java, false, "text/html"))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- RESULT ---
    inline fun <reified T> result(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun result(status: String, returnType: Class<*>? = null, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedContent = if (returnType == null || returnType == Unit::class.java) {
            listOf()
        } else {
            listOf(DocumentedContent(returnType, false))
        }
        result(status, documentedContent, openApiUpdater)
    }

    inline fun <reified T> result(status: String, contentType: String?, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, T::class.java, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun result(status: String, returnType: Class<*>?, contentType: String?, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedContent = if (returnType == null || returnType == Unit::class.java) {
            listOf()
        } else {
            listOf(DocumentedContent(returnType, false, contentType))
        }
        result(status, documentedContent, openApiUpdater)
    }

    fun result(documentedResponse: DocumentedResponse, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(documentedResponse, createUpdaterIfNotNull(applyUpdates))
    }

    fun result(status: String, content: DocumentedContent, updater: OpenApiUpdater<ApiResponse>? = null) = apply {
        result(status, listOf(content), updater)
    }

    fun result(status: String, content: List<DocumentedContent> = listOf(), updater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, updater)
    }

    @JvmOverloads
    fun result(documentedResponse: DocumentedResponse, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val responseUpdaterList = responseUpdaterListMapping.getOrSetDefault(documentedResponse.status, mutableListOf())
        componentsUpdaterList.add { it.applyDocumentedResponse(documentedResponse) }
        responseUpdaterList.add { it.applyDocumentedResponse(documentedResponse) }
        responseUpdaterList.addIfNotNull(openApiUpdater)
    }

    /** Merge the values of another documentation into this documentation */
    fun apply(other: OpenApiDocumentation) {
        other.isIgnored?.let { this.isIgnored = it }
        this.operationUpdaterList.addAll(other.operationUpdaterList)
        this.requestBodyList.addAll(other.requestBodyList)

        other.parameterUpdaterListMapping.forEach { key, value ->
            if (this.parameterUpdaterListMapping.containsKey(key)) {
                this.parameterUpdaterListMapping[key]!!.addAll(value)
            } else {
                this.parameterUpdaterListMapping[key] = value
            }
        }

        other.responseUpdaterListMapping.forEach { key, value ->
            if (this.responseUpdaterListMapping.containsKey(key)) {
                this.responseUpdaterListMapping[key]!!.addAll(value)
            } else {
                this.responseUpdaterListMapping[key] = value
            }
        }

        this.componentsUpdaterList.addAll(other.componentsUpdaterList)
    }
}

private fun <T> MutableList<OpenApiUpdater<T>>.add(applyUpdates: ApplyUpdates<T>) {
    val list = this
    list.add(createUpdater(applyUpdates))
}

private fun <T> MutableList<T>.addIfNotNull(item: T?) {
    val list = this
    item?.let { list.add(item) }
}

private fun <K, T> MutableMap<K, T>.getOrSetDefault(key: K, default: T): T {
    val value = this[key]
    if (value == null) {
        this[key] = default
        return default
    }
    return value
}

private fun List<String>.withContentTypeDefaults() =
        if (this.isEmpty()) listOf(ContentType.AUTODETECT)
        else this
