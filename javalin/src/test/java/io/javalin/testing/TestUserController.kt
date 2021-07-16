package io.javalin.testing

import io.javalin.http.CrudHandler
import io.javalin.http.Context

class TestUserController : CrudHandler {

    override fun getAll(ctx: Context) {
        ctx.result("All my users")
    }

    override fun getOne(ctx: Context, resourceId: String) {
        ctx.result("My single user: $resourceId")
    }

    override fun create(ctx: Context) {
        ctx.status(201)
    }

    override fun update(ctx: Context, resourceId: String) {
        ctx.status(204)
    }

    override fun delete(ctx: Context, resourceId: String) {
        ctx.status(204)
    }
}
