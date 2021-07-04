package io.javalin.plugin.json

import io.javalin.core.LoomUtil
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object PipedStreamUtil {

    val executorService by lazy {
        if (LoomUtil.loomAvailable) {
            LoomUtil.getExecutorService()
        } else {
            ThreadPoolExecutor(4, 32, 30L, TimeUnit.SECONDS, LinkedBlockingQueue())
        }
    }

    fun getInputStream(function: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = object : PipedInputStream(pipedOutputStream) {
            var exception: Exception? = null // we need to move the exception from the child thread to the parent thread
            override fun close() = exception?.let { throw it } ?: super.close()
        }
        executorService.submit {
            try {
                function.invoke(pipedOutputStream)
            } catch (exception: Exception) {
                pipedInputStream.exception = exception // save exception for parent thead
            } finally {
                pipedOutputStream.close()
            }
        }
        return pipedInputStream
    }

}
