/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import java.io.ByteArrayInputStream
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val cachedBytes: ByteArray = super.getInputStream().readBytes()

    override fun getInputStream(): ServletInputStream = if (chunkedTransferEncoding()) super.getInputStream() else CachedServletInputStream()

    private fun chunkedTransferEncoding(): Boolean = "chunked" == (super.getRequest() as HttpServletRequest).getHeader("Transfer-Encoding")

    private inner class CachedServletInputStream : ServletInputStream() {
        private val byteArrayInputStream: ByteArrayInputStream = ByteArrayInputStream(cachedBytes)
        override fun read(): Int = byteArrayInputStream.read()
        override fun available(): Int = byteArrayInputStream.available()
        override fun isFinished(): Boolean = available() <= 0
        override fun isReady(): Boolean = available() >= 0
        override fun setReadListener(readListener: ReadListener) {}
    }
}
