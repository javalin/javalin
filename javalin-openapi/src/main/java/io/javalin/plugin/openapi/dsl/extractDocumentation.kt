package io.javalin.plugin.openapi.dsl

import io.javalin.Javalin
import io.javalin.apibuilder.CrudFunctionHandler
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Reflection.Companion.rfl
import io.javalin.core.util.Util
import io.javalin.core.util.methodReferenceReflectionMethodName
import io.javalin.plugin.openapi.CreateSchemaOptions
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.asOpenApiDocumentation
import io.javalin.plugin.openapi.annotations.pathInfo
import io.javalin.plugin.openapi.annotations.scanForAnnotations
import io.javalin.plugin.openapi.annotations.warnUserAboutPotentialBugs
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.logging.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

fun HandlerMetaInfo.extractDocumentation(options: CreateSchemaOptions): OpenApiDocumentation {
    val documentation = document()

    options.default?.apply(documentation)

    val userDocumentation: OpenApiDocumentation = when (handler) {
        is DocumentedHandler -> (handler as DocumentedHandler).documentation
        is CrudFunctionHandler -> extractDocumentationFromCrudHandler(handler as CrudFunctionHandler)
        else -> {
            val openApiAnnotation: OpenApi? = getOpenApiAnnotationFromReference() ?: getOpenApiAnnotationFromHandler()
            openApiAnnotation?.asOpenApiDocumentation()
                    ?: extractDocumentationWithPathScanning(options)
                    ?: document()
        }
    }

    documentation.apply(userDocumentation)

    return documentation
}

private fun extractDocumentationFromCrudHandler(handler: CrudFunctionHandler): OpenApiDocumentation {
    val crudHandler = handler.crudHandler
    return if (crudHandler is DocumentedCrudHandler) {
        crudHandler.crudHandlerDocumentation.asMap()[handler.function]
                ?: throw IllegalStateException("No documentation for this function")
    } else {
        val method = rfl(handler.crudHandler).getMethodByName(handler.function.value) ?: throw NoSuchMethodException(handler.function.value)
        val openApi: OpenApi? = method.getDeclaredAnnotation(OpenApi::class.java)
        openApi?.asOpenApiDocumentation()
                ?: document()
    }
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
        methodReferenceOfHandler?.getOpenApiAnnotation()
                ?: rfl(handler).lambdaField?.getOpenApiAnnotation()
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
        val method = rfl(handler).getMethodByName("handle")!!
        method.getOpenApiAnnotation()
    } catch (e: NullPointerException) {
        null
    }
}

private val HandlerMetaInfo.methodReferenceOfHandler: Method?
    get() = rfl(handler).let { handlerReflection ->
        when {
            handlerReflection.isClass -> (handler as Class<*>).methods[0]
            handlerReflection.isKotlinMethodReference -> {
                val functionValue = handlerReflection.getFieldValue("function") as KFunction<*>
                functionValue.javaMethod
            }
            handlerReflection.isKotlinAnonymousLambda -> null // Cannot be parsed
            handlerReflection.isJavaNonStaticMethodReference -> methodReferenceOfNonStaticJavaHandler
            handlerReflection.isJavaAnonymousLambda -> null // Cannot be parsed
            else -> null
        }
    }

private val HandlerMetaInfo.methodReferenceOfNonStaticJavaHandler: Method?
    get() {
        val handlerParentClass = rfl(handler)
                .getMethodByName(methodReferenceReflectionMethodName)
                ?.parameters?.get(0)
                ?.parameterizedType as Class<*>?

        val methods = handlerParentClass
                ?.let { rfl(it) }
                ?.methodsNotDeclaredByObject
                ?: arrayOf()

        return when {
            methods.isEmpty() -> null
            methods.size == 1 -> methods[0]
            else -> {
                val methodThatMatchesHandler = findMethodByOpenApiAnnotation(methods)
                if (methodThatMatchesHandler != null) {
                    return methodThatMatchesHandler
                }

                val hasAnyMethodTheOpenApiAnnotation = methods.any { it.getOpenApiAnnotation() != null }
                if (hasAnyMethodTheOpenApiAnnotation && handlerParentClass != null) {
                    Javalin.log.warn("Unfortunately it is not possible to match the @OpenApi annotations to the handler in ${handlerParentClass.canonicalName}. " +
                            "Please add the `path` and the `method` information to the annotation, so the handler can be matched.")
                }
                null
            }
        }
    }

private fun HandlerMetaInfo.findMethodByOpenApiAnnotation(methods: Array<Method>): Method? {
    val methodsThatMatchesPath = methods.filter {
        val annotation = it.getOpenApiAnnotation() ?: return@filter false
        annotation.path == this.path
    }
    return when {
        methodsThatMatchesPath.size == 1 -> methodsThatMatchesPath[0]
        methodsThatMatchesPath.size > 1 -> methodsThatMatchesPath
                .find {
                    it.getOpenApiAnnotation()
                            ?.let { annotation -> annotation.pathInfo == pathInfo }
                            ?: false
                }
        else -> null
    }
}

fun Method.getOpenApiAnnotation(): OpenApi? {
    val result = getAnnotation(OpenApi::class.java) ?: return null
    result.warnUserAboutPotentialBugs(this.declaringClass)
    return result
}

fun Field.getOpenApiAnnotation(): OpenApi? {
    val result = getAnnotation(OpenApi::class.java) ?: return null
    result.warnUserAboutPotentialBugs(this.declaringClass)
    return result
}
