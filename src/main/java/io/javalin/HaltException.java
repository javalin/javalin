/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import javax.servlet.http.HttpServletResponse;

public class HaltException extends RuntimeException {
    public int statusCode = HttpServletResponse.SC_OK;
    public String body = "Execution halted";

    HaltException() {
    }

    HaltException(int statusCode) {
        this.statusCode = statusCode;
    }

    HaltException(String body) {
        this.body = body;
    }

    public HaltException(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

}
