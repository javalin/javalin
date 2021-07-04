package io.javalin.plugin.json

import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

object PipedStreamUtil {

    fun getInputStream(function: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)
        pipedOutputStream.use { function.invoke(it) } /** [use] closes the stream */
        return pipedInputStream
    }

}
