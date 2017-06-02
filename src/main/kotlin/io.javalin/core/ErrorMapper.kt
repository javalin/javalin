/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.ErrorHandler
import io.javalin.Request
import io.javalin.Response
import java.util.*

class ErrorMapper {

    private val errorHandlerMap: MutableMap<Int, ErrorHandler>

    init {
        this.errorHandlerMap = HashMap<Int, ErrorHandler>()
    }

    fun put(statusCode: Int, handler: ErrorHandler) {
        this.errorHandlerMap.put(statusCode, handler)
    }

    fun clear() {
        this.errorHandlerMap.clear()
    }

    fun handle(statusCode: Int, request: Request, response: Response) {
        val errorHandler = errorHandlerMap[statusCode]
        errorHandler?.handle(request, response)
    }
}
