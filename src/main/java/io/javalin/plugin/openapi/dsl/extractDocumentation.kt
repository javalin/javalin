package io.javalin.plugin.openapi.dsl

import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.core.util.lambdaField
import io.javalin.core.util.lambdaMethod
import io.javalin.plugin.openapi.CreateSchemaOptions
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.asOpenApiDocumentation
import io.javalin.plugin.openapi.annotations.scanForAnnotations
import java.util.logging.Logger

fun HandlerMetaInfo.extractDocumentation(options: CreateSchemaOptions): OpenApiDocumentation {
    val documentation = if (handler is DocumentedHandler) {
        handler.documentation
    } else {
        val openApiAnnotation: OpenApi? = getOpenApiAnnotationFromReference() ?: getOpenApiAnnotationFromHandler()
        openApiAnnotation?.asOpenApiDocumentation()
                ?: extractDocumentationWithPathScanning(options)
                ?: document()
    }

    options.default?.apply(documentation)

    return documentation
}

private fun HandlerMetaInfo.extractDocumentationWithPathScanning(options: CreateSchemaOptions): OpenApiDocumentation? {
    if (options.packagePrefixesToScan.isEmpty()) {
        return null
    }
    Util.ensureDependencyPresent(OptionalDependency.CLASS_GRAPH)
    return getPathInfo()?.let { pathInfo ->
        val documentationFromScan = scanForAnnotations(options.packagePrefixesToScan)
        documentationFromScan[pathInfo]
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromReference(): OpenApi? {
    return try {
        handler.lambdaMethod?.getAnnotation(OpenApi::class.java)
                ?: handler.lambdaField?.getAnnotation(OpenApi::class.java)
    } catch (e: NoSuchFieldException) {
        null
    } catch (e: Error) {
        return if (e::class.qualifiedName == "kotlin.reflect.jvm.internal.KotlinReflectionInternalError") {
            Logger.getGlobal()
                    .warning("Local functions, lambdas, anonymous functions and local variables with @OpenApi annotations are currently not supported. " +
                            "The annotation of the handler for \"$httpMethod $path\" will be ignored. To fix this, move the handler into a global function.")
            null
        } else {
            throw e
        }
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromHandler(): OpenApi? {
    return try {
        val method = handler::class.java.declaredMethods.find { it.name == "handle" }!!
        method.getAnnotation(OpenApi::class.java)
    } catch (e: NullPointerException) {
        null
    }
}
