package io.javalin.plugin.openapi.dsl

import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context

class DocumentedCrudHandler(
        val crudHandlerDocumentation: OpenApiCrudHandlerDocumentation,
        private val crudHandler: CrudHandler
) : CrudHandler {
    override fun getAll(ctx: Context) = crudHandler.getAll(ctx)

    override fun getOne(ctx: Context, resourceId: String) = crudHandler.getOne(ctx, resourceId)

    override fun create(ctx: Context) = crudHandler.create(ctx)

    override fun update(ctx: Context, resourceId: String) = crudHandler.update(ctx, resourceId)

    override fun delete(ctx: Context, resourceId: String) = crudHandler.delete(ctx, resourceId)
}
