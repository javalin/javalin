/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.security

import io.javalin.Handler
import io.javalin.Request
import io.javalin.Response

@FunctionalInterface
interface AccessManager {
    @Throws(Exception::class)
    fun manage(handler: Handler, request: Request, response: Response, permittedRoles: List<Role>)
}