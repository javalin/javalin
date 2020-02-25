@file:JvmName("OpenApiBuilder")

package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.handler.DocumentedCrudHandler
import io.javalin.plugin.openapi.handler.DocumentedHandler

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
