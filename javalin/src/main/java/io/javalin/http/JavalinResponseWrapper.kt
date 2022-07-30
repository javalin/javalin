package io.javalin.http

import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType.GET
import io.javalin.http.Header.ETAG
import io.javalin.http.Header.IF_NONE_MATCH
import io.javalin.http.HttpCode.NOT_MODIFIED
import io.javalin.util.Util
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.ByteArrayInputStream
import java.io.InputStream

class JavalinResponseWrapper(
    private val ctx: Context,
    private val cfg: JavalinConfig
) : HttpServletResponseWrapper(ctx.res()) {

    override fun getOutputStream() = ctx.outputStream()

    private val serverEtag by lazy { getHeader(ETAG) }
    private val clientEtag by lazy { ctx.req().getHeader(IF_NONE_MATCH) }

    fun write(resultStream: InputStream?) = when {
        resultStream == null -> {} // nothing to write (and nothing to close)
        serverEtag != null && serverEtag == clientEtag -> closeWith304(resultStream) // client etag matches, nothing to write
        serverEtag == null && cfg.http.generateEtags && ctx.method() == GET && resultStream is ByteArrayInputStream -> generateEtagWriteAndClose(resultStream)
        else -> writeToWrapperAndClose(resultStream)
    }

    private fun generateEtagWriteAndClose(resultStream: ByteArrayInputStream) {
        val generatedEtag = Util.getChecksumAndReset(resultStream)
        setHeader(ETAG, generatedEtag)
        when (generatedEtag) {
            clientEtag -> closeWith304(resultStream)
            else -> writeToWrapperAndClose(resultStream)
        }
    }

    private fun writeToWrapperAndClose(inputStream: InputStream) {
        inputStream.use { input ->
            ctx.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun closeWith304(inputStream: InputStream) {
        inputStream.use { ctx.status(NOT_MODIFIED) }
    }

}
