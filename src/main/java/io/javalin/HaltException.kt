/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

class HaltException(var statusCode: Int = 200, var body: String = "Execution halted") : RuntimeException() {
    constructor(statusCode: Int) : this() {
        this.statusCode = statusCode
    }

    constructor(body: String) : this() {
        this.body = body
    }
}
