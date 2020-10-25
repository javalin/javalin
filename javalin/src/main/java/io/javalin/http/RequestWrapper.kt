/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.JavalinConfig
import org.eclipse.jetty.http.HttpStatus
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class RequestWrapper(request: HttpServletRequest, private val config: JavalinConfig) : HttpServletRequestWrapper(request) {

    private val bodySize = this.contentLengthLong

    override fun getInputStream(): ServletInputStream {
        config.maxRequestSize?.let {
            if (bodySize > it) {
                Javalin.log?.warn("Body greater than max size $it")
                throw HttpResponseException(HttpStatus.PAYLOAD_TOO_LARGE_413, "Payload too large")
            }
        }
        return super.getInputStream()
    }
}
