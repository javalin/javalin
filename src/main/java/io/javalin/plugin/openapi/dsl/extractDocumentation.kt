package io.javalin.plugin.openapi.dsl

import io.javalin.core.event.HandlerMetaInfo
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.asOpenApiDocumentation
import java.util.logging.Logger
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

fun HandlerMetaInfo.extractDocumentation(): OpenApiDocumentation? {
    return if (handler is DocumentedHandler) {
        handler.documentation
    } else {
        val openApiAnnotation: OpenApi? = getOpenApiAnnotationFromKotlin() ?: getOpenApiAnnotationFromJava()
        openApiAnnotation?.asOpenApiDocumentation()
    }
}

private fun HandlerMetaInfo.getOpenApiAnnotationFromKotlin(): OpenApi? {
    return try {
        // It is required to work access private fields, because kotlin wraps
        // the lambda into a "SAM" class and we need to access the original lambda
        // with the annotations
        val functionValue = getFieldValue(handler, "function") as KFunction<*>
        val javaMethod = functionValue.javaMethod!!
        javaMethod.getAnnotation(OpenApi::class.java)
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

private fun HandlerMetaInfo.getOpenApiAnnotationFromJava(): OpenApi? {
    return try {
        val method = handler::class.java.declaredMethods.find { it.name == "handle" }!!
        method.getAnnotation(OpenApi::class.java)
    } catch (e: NullPointerException) {
        null
    }
}

private fun getFieldValue(obj: Any, fieldName: String): Any {
    val field = obj::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(obj)
}
