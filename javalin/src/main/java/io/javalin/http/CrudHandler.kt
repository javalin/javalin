/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

/**
 * The CrudHandler is an interface for handling the five most
 * common CRUD operations.
 */
interface CrudHandler {
    fun getAll(ctx: Context)
    fun getOne(ctx: Context, resourceId: String)
    fun create(ctx: Context)
    fun update(ctx: Context, resourceId: String)
    fun delete(ctx: Context, resourceId: String)
}

enum class CrudFunction(
        val value: String,
        val createHandler: (CrudHandler, String) -> Handler
) {
    GET_ALL("getAll", { crud, _ -> Handler { crud.getAll(it) } }),
    GET_ONE("getOne", { crud, id -> Handler { crud.getOne(it, it.pathParam(id)) } }),
    CREATE("create", { crud, _ -> Handler { crud.create(it) } }),
    UPDATE("update", { crud, id -> Handler { crud.update(it, it.pathParam(id)) } }),
    DELETE("delete", { crud, id -> Handler { crud.delete(it, it.pathParam(id)) } });
}

class CrudFunctionHandler(
        val function: CrudFunction,
        val crudHandler: CrudHandler,
        resourceId: String,
        handler: Handler = function.createHandler(crudHandler, resourceId)
) : Handler by handler
