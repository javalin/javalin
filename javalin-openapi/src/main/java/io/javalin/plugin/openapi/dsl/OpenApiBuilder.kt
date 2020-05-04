@file:JvmName("OpenApiBuilder")

package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudFunction
import io.javalin.apibuilder.CrudHandler
import io.javalin.core.util.Reflection.Companion.rfl
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

/** Converts a map of handlers and documentations into a map of documented handlers */
fun <T> documented(documentation: Map<T, OpenApiDocumentation>, handlers: Map<T, Handler>): Map<T, Handler> {
    documentation.assertSameKeys(handlers)
    return handlers.mapValues { (key, value) -> documented(documentation.getValue(key), value) }
}

internal fun documented(crudHandler: CrudHandler, handlers: Map<CrudFunction, Handler>): Map<CrudFunction, Handler> {
    return if (crudHandler is DocumentedCrudHandler) {
        documented(crudHandler.crudHandlerDocumentation.asMap(), handlers)
    } else {
        moveDocumentationFromAnnotationToHandler(crudHandler::class.java, handlers)
    }
}

internal fun moveDocumentationFromAnnotationToHandler(
        crudHandlerClass: Class<out CrudHandler>,
        handlers: Map<CrudFunction, Handler>
): Map<CrudFunction, Handler> {
    return handlers.mapValues { (key, handler) ->
        moveDocumentationFromAnnotationToHandler(crudHandlerClass, key.value, handler)
    }
}

/**
 * Moves the documentation from the annotation to a DocumentedHandler.
 * If the method is not documented, just returns the handler.
 */
fun moveDocumentationFromAnnotationToHandler(javaClass: Class<*>, methodName: String, handler: Handler): Handler {
    val method = rfl(javaClass).getMethodByName(methodName) ?: throw NoSuchMethodException(methodName)
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

private fun <K> Map<K, *>.assertSameKeys(other: Map<K, *>) {
    if (this.keys != other.keys) {
        throw IllegalArgumentException("The keys of the documentation and the handlers don't match. ${this.keys} != ${other.keys}")
    }
}

internal fun OpenApiCrudHandlerDocumentation.asMap(): Map<CrudFunction, OpenApiDocumentation> = mapOf(
        CrudFunction.GET_ALL to getAllDocumentation,
        CrudFunction.GET_ONE to getOneDocumentation,
        CrudFunction.CREATE to createDocumentation,
        CrudFunction.UPDATE to updateDocumentation,
        CrudFunction.DELETE to deleteDocumentation
)
