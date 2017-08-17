/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver

import java.io.OutputStream
import java.io.PrintWriter
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class CachedResponseWrapper(response: HttpServletResponse) : HttpServletResponseWrapper(response) {
    private val copyWriter = CopyWriter(response.outputStream)
    override fun getWriter(): PrintWriter = copyWriter
    fun getCopy() = copyWriter.copy;
    class CopyWriter(outputStream: OutputStream) : PrintWriter(outputStream) {
        var copy: String? = ""
        override fun write(s: String?) {
            copy = s
            super.write(s)
        }
    }
}
