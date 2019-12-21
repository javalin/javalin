/**
 * This file contains helper methods which convert javalin concepts
 * to OpenApi documentation.
 */
package io.javalin.plugin.openapi.dsl

import io.javalin.core.*
import io.javalin.core.PathParserSpec
import io.javalin.core.PathSegment2
import io.javalin.core.createPathParser
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.http.HandlerType
import io.javalin.plugin.openapi.CreateSchemaOptions
import io.javalin.plugin.openapi.JavalinOpenApi
import io.javalin.plugin.openapi.annotations.HttpMethod
import io.javalin.plugin.openapi.annotations.PathInfo
import io.javalin.plugin.openapi.external.schema
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(JavalinOpenApi::class.java)

fun overridePaths(
        handlerMetaInfoList: List<HandlerMetaInfo>,
        overridenPaths: List<HandlerMetaInfo>
): List<HandlerMetaInfo> {
    return overridenPaths.plus(handlerMetaInfoList.filter { handler ->
        overridenPaths.none { overridenHandler ->
            createPathParser(overridenHandler.path, true).matches(handler.path) && overridenHandler.httpMethod == handler.httpMethod
        }
    })
}

fun Components.applyMetaInfoList(handlerMetaInfoList: List<HandlerMetaInfo>, options: CreateSchemaOptions) {
    handlerMetaInfoList
            .map { it.extractDocumentation(options) }
            .filter { it.isIgnored != true }
            .flatMap { it.componentsUpdaterList }
            .applyAllUpdates(this)
}

fun Paths.applyMetaInfoList(handlerMetaInfoList: List<HandlerMetaInfo>, options: CreateSchemaOptions) {
    handlerMetaInfoList
            .filter { it.extractDocumentation(options).isIgnored != true }
            .groupBy { it.path }
            .forEach { (url, metaInfos) ->
                val pathParser = createPathParser(url, true)
                updatePath(pathParser.getOpenApiUrl()) {
                    applyMetaInfoList(options, pathParser, metaInfos)
                }
            }
}

internal fun PathParserSpec.getOpenApiUrl(): String {
    val segmentsString = segments.joinToString("/") { it.asOpenApiUrlPart() }
    return "/$segmentsString"
}

private fun PathSegment2.asOpenApiUrlPart(): String {
    return when (this) {
        is PathSegment2.Normal -> this.content
        /*
         * At the moment, OpenApi does not support wildcards. So we just leave it as it is.
         * Once it is implemented we can change this.
         */
        is PathSegment2.Wildcard -> "*"
        is PathSegment2.Parameter -> "{${this.name}}"
        is PathSegment2.MultipleSegments -> this.innerSegments.joinToString("") { it.asOpenApiUrlPart() }
    }
}

fun PathItem.applyMetaInfoList(
        options: CreateSchemaOptions,
        path: PathParserSpec,
        handlerMetaInfoList: List<HandlerMetaInfo>
) {
    handlerMetaInfoList
            .forEach { metaInfo ->
                val pathItemHttpMethod = metaInfo.httpMethod.asPathItemHttpMethod() ?: return@forEach
                operation(pathItemHttpMethod, Operation().apply {
                    applyMetaInfo(options, path, metaInfo)
                })
            }
}

fun Operation.applyMetaInfo(options: CreateSchemaOptions, path: PathParserSpec, metaInfo: HandlerMetaInfo) {
    val documentation = metaInfo.extractDocumentation(options)

    operationId = metaInfo.createDefaultOperationId(path)
    summary = metaInfo.createDefaultSummary(path)
    if (path.pathParamNames.isNotEmpty()) {
        path.pathParamNames.forEach { pathParamName ->
            updateParameter {
                name = pathParamName
                `in` = "path"
                required = true
                schema(String::class.java)
            }
        }
    }

    documentation.parameterUpdaterListMapping
            .values
            .forEach { updaters ->
                updateParameter { updaters.applyAllUpdates(this) }
            }

    if (documentation.hasRequestBodies()) {
        updateRequestBody {
            documentation.requestBodyList.applyAllUpdates(this)
        }
    }

    if (documentation.hasFormParameter() || documentation.hasFileUploads()) {
        updateRequestBody {
            applyDocumentedFormParameters(documentation.formParameterList, documentation.fileUploadList)
        }
    }

    if (documentation.hasResponses()) {
        updateResponses {
            documentation.responseUpdaterListMapping
                    .forEach { (name, updater) ->
                        updateResponse(name) {
                            updater.applyAllUpdates(this)
                        }
                    }
        }
    }

    documentation.operationUpdaterList.applyAllUpdates(this)
}

fun Paths.ensureDefaultResponse() {
    forEach { url, path ->
        path.readOperationsMap()
                .filter { (_, operation) ->
                    operation.responses.isNullOrEmpty()
                }
                .forEach { (method, operation) ->
                    operation.updateResponses {
                        updateResponse("200") {
                            this.description("Default response")
                        }
                    }

                    logger.warn(
                            "A default response was added to the documentation of $method $url"
                    )
                }
    }
}

val HandlerMetaInfo.pathInfo: PathInfo?
    get() = httpMethod.asAnnotationHttpMethod()?.let { method ->
        PathInfo(path, method)
    }

fun HandlerType.asAnnotationHttpMethod(): HttpMethod? = when (this) {
    HandlerType.GET -> HttpMethod.GET
    HandlerType.PUT -> HttpMethod.PUT
    HandlerType.POST -> HttpMethod.POST
    HandlerType.DELETE -> HttpMethod.DELETE
    HandlerType.OPTIONS -> HttpMethod.OPTIONS
    HandlerType.HEAD -> HttpMethod.HEAD
    HandlerType.PATCH -> HttpMethod.PATCH
    HandlerType.TRACE -> HttpMethod.TRACE
    else -> null
}

fun HandlerType.asPathItemHttpMethod(): PathItem.HttpMethod? = when (this) {
    HandlerType.GET -> PathItem.HttpMethod.GET
    HandlerType.PUT -> PathItem.HttpMethod.PUT
    HandlerType.POST -> PathItem.HttpMethod.POST
    HandlerType.DELETE -> PathItem.HttpMethod.DELETE
    HandlerType.OPTIONS -> PathItem.HttpMethod.OPTIONS
    HandlerType.HEAD -> PathItem.HttpMethod.HEAD
    HandlerType.PATCH -> PathItem.HttpMethod.PATCH
    HandlerType.TRACE -> PathItem.HttpMethod.TRACE
    else -> null
}

private fun HandlerMetaInfo.createDefaultOperationId(path: PathParserSpec): String {
    val metaInfo = this
    val lowerCaseMethod = metaInfo.httpMethod.toString().toLowerCase()
    val capitalizedPath = path.asReadableWords().joinToString("") { it.capitalize() }
    return lowerCaseMethod + capitalizedPath
}

private fun HandlerMetaInfo.createDefaultSummary(path: PathParserSpec): String {
    val metaInfo = this
    val capitalizedMethod = metaInfo.httpMethod.toString().toLowerCase().capitalize()
    return (listOf(capitalizedMethod) + path.asReadableWords()).joinToString(" ") { it.trim() }
}

private fun PathParserSpec.asReadableWords(): List<String> {
    val words = mutableListOf<String>()
    segments.flattenMultipleSegments().forEach { segment ->
        when (segment) {
            is PathSegment2.Normal -> words.add(segment.content.dashCaseToCamelCase())
            is PathSegment2.Wildcard -> words.addAll(arrayOf("with", "wildcard"))
            is PathSegment2.Parameter -> {
                words.add("with")
                words.add(segment.name.dashCaseToCamelCase())
            }
        }
    }
    return words
}

private fun String.dashCaseToCamelCase() = split("-")
        .map { it.toLowerCase() }
        .mapIndexed { index, s -> if (index > 0) s.capitalize() else s }
        .joinToString("")
