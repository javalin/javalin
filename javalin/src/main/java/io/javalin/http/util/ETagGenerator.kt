package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.HandlerType
import io.javalin.http.Header
import io.javalin.http.HttpStatus.NOT_MODIFIED
import io.javalin.util.Util
import java.io.ByteArrayInputStream
import java.io.InputStream

object ETagGenerator {

    /** Generates & writes etag if possible, returns true if [resultStream] has been processed, otherwise false */
    fun tryWriteEtagAndClose(generatorEnabled: Boolean, ctx: Context, resultStream: InputStream): Boolean {
        val serverEtag = ctx.res().getHeader(Header.ETAG)
        val clientEtag = ctx.req().getHeader(Header.IF_NONE_MATCH)

        if (serverEtag != null && serverEtag == clientEtag) {
            ctx.closeWith304(resultStream) // client etag matches, nothing to write)
            return true
        }

        if (serverEtag == null && generatorEnabled && ctx.method() == HandlerType.GET && resultStream is ByteArrayInputStream) {
            val generatedEtag = Util.getChecksumAndReset(resultStream)
            ctx.header(Header.ETAG, generatedEtag)

            if (generatedEtag == clientEtag) {
                ctx.closeWith304(resultStream)
                return true
            }
        }

        return false
    }

    private fun Context.closeWith304(inputStream: InputStream) {
        inputStream.use { status(NOT_MODIFIED) }
    }

}
