package io.javalin.plugin.json

import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

object PipedStreamUtil {

    val executorService by lazy { ThreadPoolExecutor(4, 32, 30L, TimeUnit.SECONDS, LinkedBlockingQueue()) }

    fun getInputStream(function: (PipedOutputStream) -> Unit): InputStream {
        val pipedOutputStream = PipedOutputStream()
        val pipedInputStream = PipedInputStream(pipedOutputStream)
        executorService.execute {
            function.invoke(pipedOutputStream)
            pipedOutputStream.close()
        }
        return pipedInputStream
    }

}
