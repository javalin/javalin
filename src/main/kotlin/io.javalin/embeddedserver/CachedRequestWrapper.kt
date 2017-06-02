/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import io.javalin.core.util.RequestUtil
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedRequestWrapper @Throws(IOException::class)
constructor(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val cachedBytes: ByteArray = RequestUtil.toByteArray(super.getInputStream())

    @Throws(IOException::class)
    override fun getInputStream(): ServletInputStream {
        if (chunkedTransferEncoding()) { // this could blow up memory if cached
            return super.getInputStream()
        }
        return CachedServletInputStream()
    }

    private fun chunkedTransferEncoding(): Boolean {
        return "chunked" == (super.getRequest() as HttpServletRequest).getHeader("Transfer-Encoding")
    }

    private inner class CachedServletInputStream : ServletInputStream() {

        private val byteArrayInputStream: ByteArrayInputStream = ByteArrayInputStream(cachedBytes)

        override fun read(): Int {
            return byteArrayInputStream.read()
        }

        override fun available(): Int {
            return byteArrayInputStream.available()
        }

        override fun isFinished(): Boolean {
            return available() <= 0
        }

        override fun isReady(): Boolean {
            return available() >= 0
        }

        override fun setReadListener(readListener: ReadListener) {}
    }
}
