/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.http

/**
 * A handler for use with [Javalin.error].
 * Is triggered by [[Context.status]] codes at the end of the request lifecycle.
 *
 * @see Context
 *
 * @see [Error mapping in docs](https://javalin.io/documentation.error-mapping)
 */
@FunctionalInterface
interface ErrorHandler : Handler {
    override fun handle(ctx: Context)
}
