/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

@FunctionalInterface
interface ErrorHandler {
    // very similar to handler, but can't throw exception
    fun handle(request: Request, response: Response)
}
