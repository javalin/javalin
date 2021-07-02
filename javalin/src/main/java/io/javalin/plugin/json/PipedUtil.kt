package io.javalin.plugin.json

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

object PipedUtil {
    fun convertInputStreamToOutputStream(byteArrayOutputStream: ByteArrayOutputStream): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)
        Thread { byteArrayOutputStream.use { it.writeTo(pipedOutputStream) } }.start()
        return pipedInputStream
    }
}
