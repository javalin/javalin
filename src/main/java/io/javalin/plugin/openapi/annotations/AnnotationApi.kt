package io.javalin.plugin.openapi.annotations

import io.javalin.Javalin
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
        val formParams: Array<OpenApiFormParam> = [],
        val requestBody: OpenApiRequestBody = OpenApiRequestBody([]),
        val composedRequestBody: OpenApiComposedRequestBody = OpenApiComposedRequestBody([]),
        val fileUploads: Array<OpenApiFileUpload> = [],
        val responses: Array<OpenApiResponse> = [],
        val security: Array<OpenApiSecurity> = [],
        /** The path of the endpoint. This will if the annotation * couldn't be found via reflection. */
        val path: String = NULL_STRING,
        /** The method of the endpoint. This will if the annotation * couldn't be found via reflection. */
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
        val allowEmptyValue: Boolean = false,
        val isRepeatable: Boolean = false
)

@Target()
annotation class OpenApiFormParam(
        val name: String,
        val type: KClass<*> = String::class,
        val required: Boolean = false
)

@Target()
annotation class OpenApiRequestBody(
        val content: Array<OpenApiContent>,
        val required: Boolean = false,
        val description: String = NULL_STRING
)

@Target()
annotation class OpenApiComposedRequestBody(
        val anyOf: Array<OpenApiContent> = [],
        val oneOf: Array<OpenApiContent> = [],
        val required: Boolean = false,
        val description: String = NULL_STRING,
        val contentType: String = ContentType.AUTODETECT
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

@Target()
annotation class OpenApiSecurity(
        val name: String,
        val scopes: Array<String> = []
)

/** Null string because annotations do not support null values */
const val NULL_STRING = "-- This string represents a null value and shouldn't be used --"

/** Null class because annotations do not support null values */
class NULL_CLASS

object ContentType {
    const val JSON = "application/json"
    const val HTML = "text/html"
    const val FORM_DATA_URL_ENCODED = "application/x-www-form-urlencoded"
    const val FORM_DATA_MULTIPART = "multipart/form-data"
    const val AUTODETECT = "AUTODETECT - Will be replaced later"
}

enum class ComposedType {
    NULL,
    ANY_OF,
    ONE_OF;
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


/** Checks if there are any potential bugs in the configuration */
fun OpenApi.warnUserAboutPotentialBugs(parentClass: Class<*>) {
    warnUserIfPathParameterIsMissingInPath(parentClass)
}

fun OpenApi.warnUserIfPathParameterIsMissingInPath(parentClass: Class<*>) {
    if (pathParams.isEmpty() || path == NULL_STRING) {
        // Nothing to check
        return
    }

    val pathParamsPlaceholders = pathParams.map { ":${it.name}" };
    val pathParamsPlaceholderNotInPath = pathParamsPlaceholders.filter { !path.contains(it) }

    if (pathParamsPlaceholderNotInPath.size > 0) {
        Javalin.log.warn(
                formatMissingPathParamsPlaceholderWarningMessage(parentClass, pathParamsPlaceholderNotInPath)
        )
    }
}

fun OpenApi.formatMissingPathParamsPlaceholderWarningMessage(parentClass: Class<*>, pathParamsPlaceholders: List<String>): String {
    val methodAsString = method.name
    val multipleParams = pathParamsPlaceholders.size > 1
    val secondSentence = if (multipleParams) {
        "The path params ${pathParamsPlaceholders.toFormattedString()} are documented, but couldn't be found in $methodAsString \"$path\"."
    } else {
        "The path param ${pathParamsPlaceholders.toFormattedString()} is documented, but couldn't be found in $methodAsString \"$path\"."
    }
    return "The `path` of one of the @OpenApi annotations on ${parentClass.canonicalName} is incorrect. " +
            secondSentence + " " +
            "Do you mean $methodAsString \"$path/${pathParamsPlaceholders.joinToString("/")}\"?"
}

fun List<String>.toFormattedString(): String {
    if (size == 1) {
        return "\"${this[0]}\""
    }
    var result = ""
    this.forEachIndexed { index, s ->
        when {
            index == lastIndex -> result += " and "
            index > 0 -> result += ", "
        }
        result += "\"$s\""
    }
    return result
}
