/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import java.io.ByteArrayOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class CachedResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {
    private val copier = ServletOutputStreamCopier(response.outputStream)
    override fun getOutputStream() = copier
    fun getCopy() = copier.streamCopy.toString()
    class ServletOutputStreamCopier(private val outputStream: ServletOutputStream) : ServletOutputStream() {
        internal val streamCopy: ByteArrayOutputStream = ByteArrayOutputStream(1024)
        override fun isReady(): Boolean = outputStream.isReady
        override fun setWriteListener(writeListener: WriteListener?) = outputStream.setWriteListener(writeListener)
        override fun write(b: Int) {
            outputStream.write(b)
            streamCopy.write(b)
        }
    }
}


