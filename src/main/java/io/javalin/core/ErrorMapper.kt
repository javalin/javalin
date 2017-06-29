/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.ErrorHandler
import java.util.*

class ErrorMapper {
    val errorHandlerMap = HashMap<Int, ErrorHandler>()
    fun handle(statusCode: Int, ctx: Context) = errorHandlerMap[statusCode]?.handle(ctx)
}
