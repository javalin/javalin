/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder

import io.javalin.http.Context
import io.javalin.http.Handler

/**
 * The CrudHandler is an interface for handling the five most
 * common CRUD operations. It's only available through the ApiBuilder.
 *
 * @see ApiBuilder
 */
interface CrudHandler {
    fun getAll(ctx: Context)
    fun getOne(ctx: Context, resourceId: String)
    fun create(ctx: Context)
    fun update(ctx: Context, resourceId: String)
    fun delete(ctx: Context, resourceId: String)
}

internal enum class CrudHandlerLambdaKey(val value: String) {
    GET_ALL("getAll"),
    GET_ONE("getOne"),
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete")
}

internal fun CrudHandler.getLambdas(resourceId: String): Map<CrudHandlerLambdaKey, Handler> {
    val crudHandler = this
    return mapOf(
            CrudHandlerLambdaKey.GET_ALL to Handler { ctx -> crudHandler.getAll(ctx) },
            CrudHandlerLambdaKey.GET_ONE to Handler { ctx -> crudHandler.getOne(ctx, ctx.pathParam(resourceId)) },
            CrudHandlerLambdaKey.CREATE to Handler { ctx -> crudHandler.create(ctx) },
            CrudHandlerLambdaKey.UPDATE to Handler { ctx -> crudHandler.update(ctx, ctx.pathParam(resourceId)) },
            CrudHandlerLambdaKey.DELETE to Handler { ctx -> crudHandler.delete(ctx, ctx.pathParam(resourceId)) }
    )
}
