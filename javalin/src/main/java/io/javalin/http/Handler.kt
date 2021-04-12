/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.http

/**
 * Main interface for endpoint actions. A handler has a void return type,
 * so you have to use [Context.result] to return data to the client.
 *
 * @see Context
 *
 * @see [Handler in documentation](https://javalin.io/documentation.handlers)
 */
fun interface Handler {
    @Throws(Exception::class)
    fun handle(ctx: Context)
}
