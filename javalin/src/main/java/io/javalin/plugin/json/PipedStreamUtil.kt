package io.javalin.plugin.json

import io.javalin.core.LoomUtil
import io.javalin.core.util.JavalinExecutors
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

object PipedStreamUtil {

    val executorService = if (LoomUtil.loomAvailable) {
        LoomUtil.getExecutorService()
    } else {
        JavalinExecutors.newCachedThreadPool("JavalinPipedStreamingThreadPool")
    }

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
