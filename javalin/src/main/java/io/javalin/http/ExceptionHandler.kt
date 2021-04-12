/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.http

/**
 * A handler for use with [Javalin.exception].
 * Is triggered when exceptions are thrown by a [Handler].
 *
 * @see Context
 *
 * @see [Exception mapping in docs](https://javalin.io/documentation.exception-mapping)
 */
fun interface ExceptionHandler<T : Exception?> {
    fun handle(exception: T, ctx: Context)
}
