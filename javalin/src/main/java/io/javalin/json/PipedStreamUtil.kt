package io.javalin.json

import io.javalin.util.ConcurrencyUtil
import io.javalin.util.function.ThrowingConsumer
import io.javalin.util.javalinLazy
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class PipedStreamExecutor(useVirtualThreads: Boolean) {

    private val executorService by javalinLazy { ConcurrencyUtil.executorService("JavalinPipedStreamingThreadPool", useVirtualThreads) }

    fun getInputStream(userCallback: ThrowingConsumer<PipedOutputStream, Exception>): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = object : PipedInputStream(pipedOutputStream) {
            var exception: Exception? = null // possible exception from child thread
            override fun close() = exception?.let { throw it } ?: super.close()
        }
        executorService.execute { // start child thread, necessary to prevent deadlock
            try {
                userCallback.accept(pipedOutputStream)
            } catch (userException: Exception) {
                pipedInputStream.exception = userException // pass exception to parent thead
            } finally {
                pipedOutputStream.close()
            }
        }
        return pipedInputStream
    }

}
