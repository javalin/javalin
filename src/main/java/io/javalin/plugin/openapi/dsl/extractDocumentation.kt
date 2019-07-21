package io.javalin.plugin.openapi.dsl

import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.*
import io.javalin.plugin.openapi.CreateSchemaOptions
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.asOpenApiDocumentation
import io.javalin.plugin.openapi.annotations.pathInfo
import io.javalin.plugin.openapi.annotations.scanForAnnotations
import java.lang.reflect.Method
import java.util.logging.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

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
    return pathInfo?.let { pathInfo ->
        val documentationFromScan = scanForAnnotations(options.packagePrefixesToScan)
        documentationFromScan[pathInfo]
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromReference(): OpenApi? {
    return try {
        methodReferenceOfHandler?.getAnnotation(OpenApi::class.java)
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

private val HandlerMetaInfo.methodReferenceOfHandler: Method?
    get() = when {
        handler.isClass -> (handler as Class<*>).declaredMethods[0]
        handler.isKotlinMethodReference -> {
            val functionValue = handler.getFieldValue("function") as KFunction<*>
            functionValue.javaMethod
        }
        handler.isKotlinAnonymousLambda -> null // Cannot be parsed
        handler.isJavaNonStaticMethodReference -> methodReferenceOfNonStaticJavaHandler
        handler.isJavaAnonymousLambda -> null // Cannot be parsed
        else -> null
    }

private val HandlerMetaInfo.methodReferenceOfNonStaticJavaHandler: Method?
    get() {
        val handlerParentClass = handler.javaClass
                .getDeclaredMethodByName(methodReferenceReflectionMethodName)
                ?.parameters?.get(0)
                ?.parameterizedType as Class<*>?

        val declaredMethods = handlerParentClass
                ?.declaredMethods
                ?: arrayOf()

        return when {
            declaredMethods.isEmpty() -> null
            declaredMethods.size == 1 -> declaredMethods[0]
            else -> {
                val methodThatMatchesHandler = findMethodByOpenApiAnnotation(declaredMethods)
                if (methodThatMatchesHandler != null) {
                    return methodThatMatchesHandler
                }

                val hasAnyMethodTheOpenApiAnnotation = declaredMethods.any { it.getAnnotation(OpenApi::class.java) != null }
                if (hasAnyMethodTheOpenApiAnnotation && handlerParentClass != null) {
                    Logger.getGlobal()
                            .warning("Unfortunately it is not possible to match the @OpenApi annotations to the handler in ${handlerParentClass.canonicalName}. " +
                                    "Please add the `path` and the `method` information to the annotation, so the handler can be matched.")
                }
                null
            }
        }
    }

private fun HandlerMetaInfo.findMethodByOpenApiAnnotation(methods: Array<Method>): Method? {
    val methodsThatMatchesPath = methods.filter {
        val annotation = it.getAnnotation(OpenApi::class.java) ?: return@filter false
        annotation.path == this.path
    }
    return when {
        methodsThatMatchesPath.size == 1 -> methodsThatMatchesPath[0]
        methodsThatMatchesPath.size > 1 -> methodsThatMatchesPath
                .find {
                    it.getAnnotation(OpenApi::class.java)
                            ?.let { annotation -> annotation.pathInfo == pathInfo }
                            ?: false
                }
        else -> null
    }
}

