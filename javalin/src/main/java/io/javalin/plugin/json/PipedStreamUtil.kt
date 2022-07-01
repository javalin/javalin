package io.javalin.plugin.json

import io.javalin.core.util.JavalinConcurrency
import io.javalin.core.util.LoomUtil
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

object PipedStreamUtil {

    val executorService = JavalinConcurrency.executorService("JavalinPipedStreamingThreadPool")

    fun getInputStream(userCallback: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = object : PipedInputStream(pipedOutputStream) {
            var exception: Exception? = null // possible exception from child thread
            override fun close() = exception?.let { throw it } ?: super.close()
        }
        executorService.execute { // start child thread, necessary to prevent deadlock
            try {
                userCallback(pipedOutputStream)
            } catch (userException: Exception) {
                pipedInputStream.exception = userException // pass exception to parent thead
            } finally {
                pipedOutputStream.close()
            }
        }
        return pipedInputStream
    }

}
