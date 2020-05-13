/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.Javalin
import java.io.ByteArrayInputStream
import javax.servlet.ReadListener
import javax.servlet.ServletInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

class CachedRequestWrapper(request: HttpServletRequest, private val bodyCacheSize: Long) : HttpServletRequestWrapper(request) {

    private val bodySize = this.contentLengthLong
    private var bodyConsumed = false

    private val cachedBytes: ByteArray by lazy { super.getInputStream().readBytes() } // don't read unless we have to

    override fun getInputStream(): ServletInputStream {
        if (bodyConsumed && bodySize > bodyCacheSize) { // consumed AND too big for cache
            Javalin.log?.error("Body already consumed, and was too big to cache. Adjust cache size with `config.requestCacheSize = newMaxSize;`")
        }
        bodyConsumed = true
        return if (bodySize > bodyCacheSize || this.getHeader("Transfer-Encoding")?.contains("chunked") == true) {
            super.getInputStream() // get raw stream if size is bigger than cache OR content is chunked
        } else {
            CachedServletInputStream(cachedBytes)
        }
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
