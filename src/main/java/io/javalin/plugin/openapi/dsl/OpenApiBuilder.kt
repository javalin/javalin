@file:JvmName("OpenApiBuilder")

package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.asOpenApiDocumentation
import java.lang.reflect.Method

/** Creates an instance of the OpenApiDocumentation */
fun document() = OpenApiDocumentation()

/** Creates an instance of the OpenApiCrudHandlerDocumentation */
fun documentCrud() = OpenApiCrudHandlerDocumentation()

/** Creates a documented Handler with a lambda */
fun documented(documentation: OpenApiDocumentation, handle: (ctx: Context) -> Unit) = DocumentedHandler(
        documentation,
        Handler { ctx -> handle(ctx) }
)

/** Creates a documented Handler */
fun documented(documentation: OpenApiDocumentation, handler: Handler) = DocumentedHandler(
        documentation,
        handler
)

/** Creates a documented CrudHandler */
fun documented(documentation: OpenApiCrudHandlerDocumentation, handler: CrudHandler) = DocumentedCrudHandler(documentation, handler)

/**
 * Moves the documentation from the annotation to a DocumentedHandler.
 * If the method is not documented, just returns the handler.
 */
fun moveDocumentationFromAnnotationToHandler(javaClass: Class<*>, methodName: String, handler: Handler): Handler {
    val method = javaClass.declaredMethods.find { it.name === methodName } ?: throw NoSuchMethodException(methodName)
    return moveDocumentationFromAnnotationToHandler(method, handler)
}

/**
 * Moves the documentation from the annotation to a DocumentedHandler.
 * If the method is not documented, just returns the handler.
 */
fun moveDocumentationFromAnnotationToHandler(methodWithAnnotation: Method, handler: Handler): Handler {
    val openApi: OpenApi? = methodWithAnnotation.getDeclaredAnnotation(OpenApi::class.java)
    return openApi?.asOpenApiDocumentation()?.let { documented(it, handler) } ?: handler
}
