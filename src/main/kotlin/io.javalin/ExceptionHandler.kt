/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

@FunctionalInterface
interface ExceptionHandler<in T : Exception> {
    fun handle(exception: T, request: Request, response: Response)
}
