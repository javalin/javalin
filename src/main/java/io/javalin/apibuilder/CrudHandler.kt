/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.apibuilder

import io.javalin.Context

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
