/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import javax.servlet.http.HttpServletResponse

class HaltException : RuntimeException {
    var statusCode = HttpServletResponse.SC_OK
    var body = "Execution halted"

    constructor() {}

    constructor(statusCode: Int) {
        this.statusCode = statusCode
    }

    constructor(body: String) {
        this.body = body
    }

    constructor(statusCode: Int, body: String) {
        this.statusCode = statusCode
        this.body = body
    }

}
