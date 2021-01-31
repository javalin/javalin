package io.javalin.routeoverview

import io.javalin.apibuilder.CrudHandler
import io.javalin.http.Context

class CrudHandlerImpl : CrudHandler {
    override fun getAll(ctx: Context) {}
    override fun getOne(ctx: Context, resourceId: String) {}
    override fun create(ctx: Context) {}
    override fun update(ctx: Context, resourceId: String) {}
    override fun delete(ctx: Context, resourceId: String) {}
}
