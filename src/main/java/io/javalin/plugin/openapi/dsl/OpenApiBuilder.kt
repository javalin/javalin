@file:JvmName("OpenApiBuilder")

package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler
import io.javalin.apibuilder.CrudHandlerLambdaKey
import io.javalin.http.Context
import io.javalin.http.Handler

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
    if (documentation.keys != handlers.keys) {
        throw IllegalArgumentException("The keys of the documentation and the handlers don't match. ${documentation.keys} != ${handlers.keys}")
    }
    return handlers.mapValues { (key, value) -> documented(documentation.getValue(key), value) }
}

internal fun documented(crudHandler: CrudHandler, handlers: Map<CrudHandlerLambdaKey, Handler>): Map<CrudHandlerLambdaKey, Handler> {
    return if (crudHandler is DocumentedCrudHandler) {
        documented(crudHandler.crudHandlerDocumentation, handlers)
    } else {
        handlers
    }
}

internal fun documented(crudHandlerDocumentation: OpenApiCrudHandlerDocumentation, handlers: Map<CrudHandlerLambdaKey, Handler>): Map<CrudHandlerLambdaKey, Handler> {
    val documentations = mapOf(
            CrudHandlerLambdaKey.GET_ALL to crudHandlerDocumentation.getAllDocumentation,
            CrudHandlerLambdaKey.GET_ONE to crudHandlerDocumentation.getOneDocumentation,
            CrudHandlerLambdaKey.CREATE to crudHandlerDocumentation.createDocumentation,
            CrudHandlerLambdaKey.UPDATE to crudHandlerDocumentation.updateDocumentation,
            CrudHandlerLambdaKey.DELETE to crudHandlerDocumentation.deleteDocumentation
    )
    return documented(documentations, handlers)
}
