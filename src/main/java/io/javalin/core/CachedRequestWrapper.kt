/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.core.util.Header
import java.io.ByteArrayInputStream
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedRequestWrapper(request: HttpServletRequest, private val maxCacheSize: Long) : HttpServletRequestWrapper(request) {

    private val size = request.contentLengthLong
    private val chunkedTransferEncoding by lazy {
        request.getHeader(Header.TRANSFER_ENCODING)?.contains("chunked") ?: false
    }

    // Do not read unless we have to
    private val cachedBytes: ByteArray by lazy { super.getInputStream().readBytes() }

    override fun getInputStream(): ServletInputStream =
            if (chunkedTransferEncoding || maxCacheSize < size) {
                super.getInputStream()
            } else {
                CachedServletInputStream(cachedBytes)
            }

    private inner class CachedServletInputStream(cachedBytes: ByteArray) : ServletInputStream() {
        private val byteArrayInputStream: ByteArrayInputStream = ByteArrayInputStream(cachedBytes)
        override fun read(): Int = byteArrayInputStream.read()
        override fun available(): Int = byteArrayInputStream.available()
        override fun isFinished(): Boolean = available() <= 0
        override fun isReady(): Boolean = available() >= 0
        override fun setReadListener(readListener: ReadListener) {}
    }
}
