package io.javalin.plugin.openapi.dsl

import io.javalin.plugin.openapi.annotations.ComposedType
import io.javalin.plugin.openapi.annotations.ContentType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
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
    val formParameterList = mutableListOf<DocumentedFormParameter>()
    val fileUploadList = mutableListOf<DocumentedFileUpload>()

    fun hasRequestBodies(): Boolean = requestBodyList.isNotEmpty()
    fun hasResponses(): Boolean = responseUpdaterListMapping.values.flatten().isNotEmpty()
    fun hasFormParameter(): Boolean = formParameterList.isNotEmpty()
    fun hasFileUploads(): Boolean = fileUploadList.isNotEmpty()

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
    @JvmSynthetic
    inline fun <reified T> pathParam(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        pathParam(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun pathParam(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("path", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- QUERY PARAM ---
    @JvmSynthetic
    inline fun <reified T> queryParam(name: String, isRepeatable: Boolean = false, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        queryParam(name, T::class.java, isRepeatable, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun queryParam(name: String, clazz: Class<*>, isRepeatable: Boolean = false, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("query", name, clazz)
        param(documentedQueryParameter, isRepeatable, openApiUpdater)
    }

    // --- HEADER ---
    @JvmSynthetic
    inline fun <reified T> header(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        header(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun header(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("header", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- COOKIE ---
    @JvmSynthetic
    inline fun <reified T> cookie(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        cookie(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun cookie(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("cookie", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- PARAM ---
    fun param(documentedParameter: DocumentedParameter, isRepeatable: Boolean = false, applyUpdates: ApplyUpdates<Parameter>? = null) = apply {
        param(documentedParameter, isRepeatable, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun param(documentedParameter: DocumentedParameter, isRepeatable: Boolean = false, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val parameterUpdaterList = parameterUpdaterListMapping.getOrSetDefault(documentedParameter.name, mutableListOf())
        componentsUpdaterList.add { it.applyDocumentedParameter(documentedParameter) }
        parameterUpdaterList.add {
            if (isRepeatable)
                it.applyRepeatableDocumentedParameter(documentedParameter)
            else
                it.applyDocumentedParameter(documentedParameter)
        }
        parameterUpdaterList.addIfNotNull(openApiUpdater)
    }

    // --- FORM PARAM ---
    @JvmSynthetic
    inline fun <reified T> formParam(name: String, required: Boolean = false): OpenApiDocumentation = apply {
        formParam(name, T::class.java, required)
    }

    fun formParam(name: String, clazz: Class<*>, required: Boolean = false) = apply {
        formParam(DocumentedFormParameter(name, clazz, required))
    }

    fun formParam(formParameter: DocumentedFormParameter) = apply {
        formParameterList.add(formParameter)
    }

    // --- FORM PARAM BODY ---
    @JvmSynthetic
    inline fun <reified T> formParamBody(noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        formParamBody(T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmSynthetic
    inline fun <reified T> formParamBody(contentType: String? = null, noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        formParamBody(T::class.java, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun formParamBody(clazz: Class<*>, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        formParamBody(clazz, null, openApiUpdater)
    }

    fun formParamBody(clazz: Class<*>, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(clazz, contentType ?: ContentType.FORM_DATA_URL_ENCODED, openApiUpdater)
    }

    // --- UPLOADED FILE ---
    fun uploadedFile(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFile(name, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun uploadedFile(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        fileUploadList.add(DocumentedFileUpload(name))
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- UPLOADED FILES ---
    fun uploadedFiles(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFiles(name, createUpdaterIfNotNull(applyUpdates))
    }

    @JvmOverloads
    fun uploadedFiles(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        fileUploadList.add(DocumentedFileUpload(name, true))
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- BODY ---
    @JvmSynthetic
    inline fun <reified T> body(noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(T::class.java, null, applyUpdates)
    }

    @JvmSynthetic
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
    fun body(composition: Composition, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(composition.content, openApiUpdater, contentType, composition.type)
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
    fun body(content: List<DocumentedContent>, openApiUpdater: OpenApiUpdater<RequestBody>? = null, contentType: String? = null, composedType: ComposedType = ComposedType.NULL) = apply {
        val documentedBody = DocumentedRequestBody(content, contentType, composedType)
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
    @JvmSynthetic
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
    @JvmSynthetic
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
    @JvmSynthetic
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

    @JvmSynthetic
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

    @JvmOverloads
    fun result(status: String, composition: Composition.OneOf, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, composition.content, createUpdaterIfNotNull(applyUpdates))
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
        this.fileUploadList.addAll(other.fileUploadList)
        this.formParameterList.addAll(other.formParameterList)
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
