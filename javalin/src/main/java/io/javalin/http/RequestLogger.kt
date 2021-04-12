/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.http

/**
 * Interface for logging requests.
 *
 * @see Context
 *
 * @see [RequestLogger in documentation](https://javalin.io/documentation.request-loggers)
 */
fun interface RequestLogger {
    @Throws(Exception::class)
    fun handle(ctx: Context, executionTimeMs: Float)
}
