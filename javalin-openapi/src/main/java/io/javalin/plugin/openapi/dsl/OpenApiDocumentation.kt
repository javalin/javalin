package io.javalin.plugin.openapi.dsl

import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.ComposedType
import io.javalin.plugin.openapi.annotations.ContentType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse

/**
 * [OpenApiDocumentation] contains OpenAPI documentation for a [Handler].
 *
 * This class should be modified by the available functions and passed into [io.javalin.plugin.openapi.dsl.documented] which will include it
 * in the generated documentation.
 */
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

    /**
     * Documents the handler using the [Operation] provided by the input [applyUpdates].
     *
     * @param applyUpdates A function that returns a Swagger [Operation].
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun operation(applyUpdates: ApplyUpdates<Operation>) = apply {
        operation(createUpdater(applyUpdates))
    }

    /**
     * Documents the handler using the [Operation] provided by the input [openApiUpdater].
     *
     * @param openApiUpdater A function that returns a Swagger [Operation].
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun operation(openApiUpdater: OpenApiUpdater<Operation>) = apply {
        operationUpdaterList.add(openApiUpdater)
    }

    // --- PATH PARAM ---

    /**
     * Documents that the handler can receive a path parameter.
     *
     * @param name The name of the parameter.
     * @param applyUpdates A function that allows the underlying Swagger [Parameter] to modified directly.
     * @param T The class of the parameter.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> pathParam(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        pathParam(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a path parameter.
     *
     * @param name The name of the parameter.
     * @param clazz The class of the parameter.
     * @param openApiUpdater A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun pathParam(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("path", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- QUERY PARAM ---

    /**
     * Documents that the handler can receive a query parameter.
     *
     * [queryParam] can be called multiple times with different [name]s that represent different parameters, all will be added to the generated documentation.
     *
     * @param name The name of the parameter.
     * @param isRepeatable Whether the parameter is repeatable.
     * @param applyUpdates A function that allows the underlying Swagger [Parameter] to modified directly.
     * @param T The class of the parameter.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> queryParam(name: String, isRepeatable: Boolean = false, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        queryParam(name, T::class.java, isRepeatable, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a query parameter.
     *
     * [queryParam] can be called multiple times with different [name]s that represent different parameters, all will be added to the generated documentation.
     *
     * @param name The name of the parameter.
     * @param clazz The class of the parameter.
     * @param isRepeatable Whether the parameter is repeatable.
     * @param openApiUpdater A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun queryParam(name: String, clazz: Class<*>, isRepeatable: Boolean = false, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("query", name, clazz)
        param(documentedQueryParameter, isRepeatable, openApiUpdater)
    }

    // --- HEADER ---

    /**
     * Documents that the handler has a header.
     *
     * [header] can be called multiple times with different [name]s that represent different headers, all will be added to the generated documentation.
     *
     * @param name The name of the header.
     * @param applyUpdates A function that allows the underlying Swagger [Parameter] to modified directly.
     * @param T The class that represents the header.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> header(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        header(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler has a header.
     *
     * [header] can be called multiple times with different [name]s that represent different headers, all will be added to the generated documentation.
     *
     * @param name The name of the header.
     * @param clazz The class that represents the header.
     * @param openApiUpdater A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun header(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("header", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- COOKIE ---

    /**
     * Documents that the handler can receive a cookie.
     *
     * [cookie] can be called multiple times with different [name]s that represent different cookies, all will be added to the generated documentation.
     *
     * @param name The name of the cookie.
     * @param applyUpdates A function that allows the underlying Swagger [Parameter] to modified directly.
     * @param T The class that represents the cookie.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> cookie(name: String, noinline applyUpdates: ApplyUpdates<Parameter>? = null): OpenApiDocumentation = apply {
        cookie(name, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a cookie.
     *
     * [cookie] can be called multiple times with different [name]s that represent different cookies, all will be added to the generated documentation.
     *
     * @param name The name of the cookie.
     * @param clazz The class that represents the cookie.
     * @param openApiUpdater A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun cookie(name: String, clazz: Class<*>, openApiUpdater: OpenApiUpdater<Parameter>? = null) = apply {
        val documentedQueryParameter = DocumentedParameter("cookie", name, clazz)
        param(documentedQueryParameter, openApiUpdater = openApiUpdater)
    }

    // --- PARAM ---

    /**
     * Documents that the handler can receive a parameter detailed by the input [DocumentedParameter].
     *
     * [param] can be called multiple times with different [DocumentedParameter]s that represent different parameters, all will be added to the generated documentation.
     *
     * @param documentedParameter The [DocumentedParameter] the handler can receive.
     * @param isRepeatable Whether the parameter is repeatable.
     * @param applyUpdates A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun param(documentedParameter: DocumentedParameter, isRepeatable: Boolean = false, applyUpdates: ApplyUpdates<Parameter>? = null) = apply {
        param(documentedParameter, isRepeatable, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a parameter detailed by the input [DocumentedParameter].
     *
     * [param] can be called multiple times with different [DocumentedParameter]s that represent different parameters, all will be added to the generated documentation.
     *
     * @param documentedParameter The [DocumentedParameter] the handler can receive.
     * @param isRepeatable Whether the parameter is repeatable.
     * @param openApiUpdater A function that allows the underlying Swagger [Parameter] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
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

    /**
     * Documents that the handler can receive a form parameter.
     *
     * [formParam] can be called multiple times with different [name]s that represent different form parameters, all will be added to the generated documentation.
     *
     * @param name The name of the parameter.
     * @param required Whether the parameter is required.
     * @param T The class of the parameter.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> formParam(name: String, required: Boolean = false): OpenApiDocumentation = apply {
        formParam(name, T::class.java, required)
    }

    /**
     * Documents that the handler can receive a form parameter.
     *
     * [formParam] can be called multiple times with different [name]s that represent different form parameters, all will be added to the generated documentation.
     *
     * @param name The name of the parameter.
     * @param clazz The class of the parameter.
     * @param required Whether the parameter is required.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun formParam(name: String, clazz: Class<*>, required: Boolean = false) = apply {
        formParam(DocumentedFormParameter(name, clazz, required))
    }

    /**
     * Documents that the handler can receive a form parameter detailed by the input [DocumentedFormParameter].
     *
     * [formParam] can be called multiple times with different [DocumentedFormParameter]s that represent different form parameters, all will be added to the generated documentation.
     *
     * @param formParameter The [DocumentedFormParameter] the handler can receive.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun formParam(formParameter: DocumentedFormParameter) = apply {
        formParameterList.add(formParameter)
    }

    // --- FORM PARAM BODY ---

    /**
     * Documents that the handler can receive a request body with a content type of `application/x-www-form-urlencoded` in the form of [T].
     *
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     * @param T The class that the handler can receive in its request body.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> formParamBody(noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        formParamBody(T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a request body with the input [contentType] in the form of [T].
     *
     * If no [contentType] is provided then a content type of `application/x-www-form-urlencoded` is used.
     *
     * @param contentType The content type of the request body.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     * @param T The class that the handler can receive in its request body.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> formParamBody(contentType: String? = null, noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        formParamBody(T::class.java, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a request body with a content type of `application/x-www-form-urlencoded` in the form of [clazz].
     *
     * @param clazz The class that the handler can receive in its request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun formParamBody(clazz: Class<*>, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        formParamBody(clazz, null, openApiUpdater)
    }

    /**
     * Documents that the handler can receive a request body with the input [contentType] in the form of [clazz].
     *
     * If no [contentType] is provided then a content type of `application/x-www-form-urlencoded` is used.
     *
     * @param clazz The class that the handler can receive in its request body.
     * @param contentType The content type of the request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun formParamBody(clazz: Class<*>, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(clazz, contentType ?: ContentType.FORM_DATA_URL_ENCODED, openApiUpdater)
    }

    // --- UPLOADED FILE ---

    /**
     * Documents that the handler can receive a file with the input [name].
     *
     * [uploadedFile] can be called multiple times with different [name]s that represent different files, all will be added to the generated documentation.
     *
     * @param name The name of the uploaded files.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun uploadedFile(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFile(name, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a file with the input [name].
     *
     * [uploadedFile] can be called multiple times with different [name]s that represent different files, all will be added to the generated documentation.
     *
     * @param name The name of the uploaded files.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun uploadedFile(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        fileUploadList.add(DocumentedFileUpload(name))
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- UPLOADED FILES ---

    /**
     * Documents that the handler can receive files with the input [name].
     *
     * [uploadedFiles] can be called multiple times with different [name]s that represent different files, all will be added to the generated documentation.
     *
     * @param name The name of the uploaded files.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun uploadedFiles(name: String, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        uploadedFiles(name, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive files with the input [name].
     *
     * [uploadedFiles] can be called multiple times with different [name]s that represent different files, all will be added to the generated documentation.
     *
     * @param name The name of the uploaded files.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun uploadedFiles(name: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        fileUploadList.add(DocumentedFileUpload(name, true))
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- BODY ---

    /**
     * Documents that the handler can receive a request body in the form of the [T].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     * @param T The class that the handler can receive in its request body.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> body(noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(T::class.java, null, applyUpdates)
    }

    /**
     * Documents that the handler can receive a request body in the form of the [T].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param contentType The content type of the request body.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     * @param T The class that the handler can receive in its request body.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> body(contentType: String? = null, noinline applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(T::class.java, contentType, applyUpdates)
    }

    /**
     * Documents that the handler can receive a request body in the form of [returnType].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param returnType The class that the handler can receive in its request body.
     * @param contentType The content type of the request body.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun body(returnType: Class<*>, contentType: String? = null, applyUpdates: ApplyUpdates<RequestBody>? = null) = apply {
        body(returnType, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a request body in the form of [returnType].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param returnType The class that the handler can receive in its request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun body(returnType: Class<*>, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(returnType, null, openApiUpdater)
    }

    /**
     * Documents that the handler can receive a request body in the form of the input [composition].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param composition The [Composition] that the handler can receive in its request body.
     * @param contentType The content type of the request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun body(composition: Composition, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(composition.content, openApiUpdater, contentType, composition.type)
    }

    /**
     * Documents that the handler can receive a request body in the form of [returnType].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param returnType The class that the handler can receive in its request body.
     * @param contentType The content type of the request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun body(returnType: Class<*>, contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val documentedContent = listOf(DocumentedContent(returnType, false, contentType))
        body(documentedContent, openApiUpdater)
    }

    /**
     * Documents that the handler can receive a request body in the form of the input [Schema].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param schema The [Schema] that the handler can receive in its request body.
     * @param contentType The content type of the request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun body(schema: Schema<*>, contentType: String, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        val documentedContent = listOf(DocumentedContent(schema, contentType))
        body(documentedContent, openApiUpdater)
    }

    /**
     * Documents that the handler can receive a request body in the form of the input [DocumentedRequestBody].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param content The [DocumentedContent]s that the handler can receive in its request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     * @param contentType The content type of the request body.
     * @param composedType The [ComposedType] of the request body's content.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun body(content: List<DocumentedContent>, openApiUpdater: OpenApiUpdater<RequestBody>? = null, contentType: String? = null, composedType: ComposedType = ComposedType.NULL) = apply {
        val documentedBody = DocumentedRequestBody(content, contentType, composedType)
        body(documentedBody, openApiUpdater)
    }

    /**
     * Documents that the handler can receive a request body in the form of the input [DocumentedRequestBody].
     *
     * [body] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param documentedBody The [DocumentedRequestBody] that the handler can receive.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun body(documentedBody: DocumentedRequestBody, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        componentsUpdaterList.add { it.applyDocumentedRequestBody(documentedBody) }
        requestBodyList.add { it.applyDocumentedRequestBody(documentedBody) }
        requestBodyList.addIfNotNull(openApiUpdater)
    }

    // --- BODY AS BYTES ---

    /**
     * Documents that the handler can receive a request body with the input [contentType] in the form of bytes.
     *
     * [bodyAsBytes] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param contentType The content type of the request body.
     * @param applyUpdates A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun bodyAsBytes(contentType: String? = null, applyUpdates: ApplyUpdates<RequestBody>?) = apply {
        bodyAsBytes(contentType, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can receive a request body with the input [contentType] in the form of bytes.
     *
     * [bodyAsBytes] can be called multiple times that represent different request bodies, all will be added to the generated documentation.
     *
     * @param contentType The content type of the request body.
     * @param openApiUpdater A function that allows the underlying Swagger [RequestBody] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun bodyAsBytes(contentType: String? = null, openApiUpdater: OpenApiUpdater<RequestBody>? = null) = apply {
        body(ByteArray::class.java, contentType, openApiUpdater)
    }

    // --- JSON ARRAY ---

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `application/json` representing an array of [T].
     *
     * A schema will be added to represent class [T] in the documentation.
     *
     * [jsonArray] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     * @param T The class that the documented response body contains.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> jsonArray(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        jsonArray(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `application/json` representing an array of [returnType].
     *
     * A schema will be added to represent class [T] in the documentation.
     *
     * [jsonArray] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param returnType The class that the documented response body contains.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun jsonArray(status: String, returnType: Class<*>, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(returnType, true, ContentType.JSON))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- JSON ---

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `application/json` in the form of [T].
     *
     * A schema will be added to represent class [T] in the documentation.
     *
     * [json] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     * @param T The class that the documented response body contains.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> json(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        json(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `application/json` in the form of [returnType].
     *
     * A schema will be added to represent class [returnType] in the documentation.
     *
     * [json] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param returnType The class that the documented response body contains.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun json(status: String, returnType: Class<*>, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(returnType, false, ContentType.JSON))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- HTML ---

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `text/html`
     *
     * [html] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun html(status: String, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        html(status, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain `text/html`
     *
     * [html] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun html(status: String, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val content = listOf(DocumentedContent(String::class.java, false, "text/html"))
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, openApiUpdater)
    }

    // --- RESULT ---

    /**
     * Documents that the handler can return the input [status] code where the response body will contain [T].
     *
     * [result] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     * @param T The class that the documented response body contains.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> result(status: String, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, T::class.java, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain [returnType].
     *
     * [result] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param returnType The class that the documented response body contains.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun result(status: String, returnType: Class<*>? = null, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedContent = if (returnType == null || returnType == Unit::class.java) {
            listOf()
        } else {
            listOf(DocumentedContent(returnType, false))
        }
        result(status, documentedContent, openApiUpdater)
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain [contentType] in the form of [T].
     *
     * [result] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     * @param T The class that the documented response body contains.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmSynthetic
    inline fun <reified T> result(status: String, contentType: String?, noinline applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, T::class.java, contentType, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain [contentType] in the form of [returnType].
     *
     * [result] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param returnType The class that the documented response body contains.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun result(status: String, returnType: Class<*>?, contentType: String?, openApiUpdater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedContent = if (returnType == null || returnType == Unit::class.java) {
            listOf()
        } else {
            listOf(DocumentedContent(returnType, false, contentType))
        }
        result(status, documentedContent, openApiUpdater)
    }

    /**
     * Documents that the handler can return the input [status] code where the response body could contain different classes represented by [Composition.OneOf].
     *
     * [result] can be called multiple times with different [status] codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param composition The classes that could be contained in the documented response body.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    @JvmOverloads
    fun result(status: String, composition: Composition.OneOf, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(status, composition.content, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [DocumentedResponse].
     *
     * [result] can be called multiple times with different [DocumentedResponse]s that represent different status codes, all will be added to the generated documentation.
     *
     * @param documentedResponse The [DocumentedResponse] to use.
     * @param applyUpdates A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun result(documentedResponse: DocumentedResponse, applyUpdates: ApplyUpdates<ApiResponse>? = null) = apply {
        result(documentedResponse, createUpdaterIfNotNull(applyUpdates))
    }

    /**
     * Documents that the handler can return the input [status] code where the response body will contain the input [DocumentedContent].
     *
     * [result] can be called multiple times with different [DocumentedResponse]s that represent different status codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param content The [DocumentedContent] that the documented response body contains.
     * @param updater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun result(status: String, content: DocumentedContent, updater: OpenApiUpdater<ApiResponse>? = null) = apply {
        result(status, listOf(content), updater)
    }

    /**
     * Documents that the handler can return the input [status] code where the response body could contain the input [DocumentedContent]s.
     *
     * [result] can be called multiple times with different [DocumentedResponse]s that represent different status codes, all will be added to the generated documentation.
     *
     * @param status The status code to document.
     * @param content The [DocumentedContent]s that the documented response body could contain.
     * @param updater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
    fun result(status: String, content: List<DocumentedContent> = listOf(), updater: OpenApiUpdater<ApiResponse>? = null) = apply {
        val documentedResponse = DocumentedResponse(status, content)
        result(documentedResponse, updater)
    }

    /**
     * Documents that the handler can return the input [DocumentedResponse].
     *
     * [result] can be called multiple times with different [DocumentedResponse]s that represent different status codes, all will be added to the generated documentation.
     *
     * @param documentedResponse The [DocumentedResponse] to use.
     * @param openApiUpdater A function that allows the underlying Swagger [ApiResponse] to modified directly.
     *
     * @return The current [OpenApiDocumentation] for further modification.
     */
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
