package io.javalin.plugin.openapi.handler

import io.javalin.apibuilder.CrudHandler
import io.javalin.apibuilder.CrudHandlerType
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.plugin.openapi.dsl.OpenApiCrudHandlerDocumentation

data class DocumentedCrudHandler(
        val crudHandlerDocumentation: OpenApiCrudHandlerDocumentation,
        private val crudHandler: CrudHandler
) : CrudHandler {
    override fun getAll(ctx: Context) = crudHandler.getAll(ctx)
    override fun getOne(ctx: Context, resourceId: String) = crudHandler.getOne(ctx, resourceId)
    override fun create(ctx: Context) = crudHandler.create(ctx)
    override fun update(ctx: Context, resourceId: String) = crudHandler.update(ctx, resourceId)
    override fun delete(ctx: Context, resourceId: String) = crudHandler.delete(ctx, resourceId)

    override fun asMap(resourceId: String): Map<CrudHandlerType, Handler> {
        val docKeys = crudHandlerDocumentation.asMap()
        return crudHandler.asMap(resourceId).mapValues { entry -> DocumentedHandler(docKeys[entry.key]!!, entry.value) }
    }
}
