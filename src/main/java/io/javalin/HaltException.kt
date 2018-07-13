/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

/**
 * Exception which is handled before all other exceptions. Can be used to short-circuit the request cycle.
 * @see <a href="https://javalin.io/documentation#exception-mapping">HaltException in docs</a>
 */
class HaltException(var statusCode: Int, var body: String) : RuntimeException() {

    constructor(statusCode: Int) : this(statusCode, "") {
        this.statusCode = statusCode
    }
}
