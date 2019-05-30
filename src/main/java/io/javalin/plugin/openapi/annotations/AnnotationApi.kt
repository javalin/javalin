package io.javalin.plugin.openapi.annotations

import kotlin.reflect.KClass

/**
 * Provide metadata for the generation of the open api documentation to the annotated Handler.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
annotation class OpenApi(
        /** Ignore the endpoint in the open api documentation */
        val ignore: Boolean = false,
        val summary: String = NULL_STRING,
        val description: String = NULL_STRING,
        val operationId: String = NULL_STRING,
        val deprecated: Boolean = false,
        val tags: Array<String> = [],
        val cookies: Array<OpenApiParam> = [],
        val headers: Array<OpenApiParam> = [],
        val pathParams: Array<OpenApiParam> = [],
        val queryParams: Array<OpenApiParam> = [],
        val requestBody: OpenApiRequestBody = OpenApiRequestBody([]),
        val fileUploads: Array<OpenApiFileUpload> = [],
        val responses: Array<OpenApiResponse> = [],
        /**
         * The path of the endpoint. This will only be used if class scanning is activated and the annotation
         * couldn't be found via reflection.
         */
        val path: String = NULL_STRING,
        /**
         * The method of the endpoint. This will only be used if class scanning is activated and the annotation
         * couldn't be found via annotation.
         */
        val method: HttpMethod = HttpMethod.GET
)

@Target()
annotation class OpenApiResponse(
        val status: String,
        val content: Array<OpenApiContent> = [],
        val description: String = NULL_STRING
)

@Target()
annotation class OpenApiParam(
        val name: String,
        val type: KClass<*> = String::class,
        val description: String = NULL_STRING,
        val deprecated: Boolean = false,
        val required: Boolean = false,
        val allowEmptyValue: Boolean = false
)

@Target()
annotation class OpenApiRequestBody(
        val content: Array<OpenApiContent>,
        val required: Boolean = false,
        val description: String = NULL_STRING
)

@Target()
annotation class OpenApiFileUpload(
        val name: String,
        val isArray: Boolean = false,
        val description: String = NULL_STRING,
        val required: Boolean = false
)

@Target()
annotation class OpenApiContent(
        val from: KClass<*> = NULL_CLASS::class,
        /** Whenever the schema should be wrapped in an array */
        val isArray: Boolean = false,
        val type: String = ContentType.AUTODETECT
)

/** Null string because annotations do not support null values */
const val NULL_STRING = "-- This string represents a null value and shouldn't be used --"

/** Null class because annotations do not support null values */
class NULL_CLASS

object ContentType {
    const val JSON = "application/json"
    const val HTML = "text/html"
    const val AUTODETECT = "AUTODETECT - Will be replaced later"
}

enum class HttpMethod {
    POST,
    GET,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS,
    TRACE;
}

data class PathInfo(val path: String, val method: HttpMethod)

val OpenApi.pathInfo get() = PathInfo(path, method)

